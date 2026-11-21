"""Read-only reference-data gate; no DB, network, seed or demo generator calls.

The opt-in demo-reference-2026 profile verifies membership of stable catalog keys,
not historical row counts, IDs, prices, flags or current business fixtures.
"""
from collections import Counter
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import re


PROFILE = 'demo-reference-2026'


def load_requirements(root, profile):
    if profile is None:
        return None
    if profile != PROFILE:
        raise ValueError('Unknown reference-data profile')
    root = Path(root)
    paths = {
        'subjects': 'database/bootstrap/scms_demo_reference.sql',
        'rooms': 'database/migrations/13_room_catalog_postgres.sql',
        'holidays': 'database/seed_holidays_catalog_2026.sql',
    }
    texts = {key: (root / path).read_text(encoding='utf-8-sig') for key, path in paths.items()}
    subject_block = texts['subjects'].split('INSERT INTO _scms_subjects VALUES', 1)[1].split(';', 1)[0]
    room_block = texts['rooms'].split('INSERT INTO _room_catalog VALUES', 1)[1].split(';', 1)[0]
    holiday_block = texts['holidays'].split('WITH catalog(holiday_date, holiday_name, tag) AS (', 1)[1].split('), inserted AS', 1)[0]
    subjects = re.findall(r"\(\s*\d+,\s*'[^']*',\s*'([^']+)'", subject_block)
    rooms = re.findall(r"\(\s*'(C[0-9]+-[A-Z0-9]+-[A-Z0-9]+)'", room_block)
    holidays = re.findall(r"DATE '(\d{4}-\d{2}-\d{2})'", holiday_block)
    # Empty/partial parser output must never turn missing requirements into PASS.
    for key, values, block in [('subjects', subjects, subject_block), ('rooms', rooms, room_block),
                               ('holidays', holidays, holiday_block)]:
        row_count = len(re.findall(r'^\s*\(', block, re.MULTILINE))
        if not values or len(values) != len(set(values)) or len(values) != row_count:
            raise ValueError('Reference profile source invalid: ' + key)
    for value in holidays:
        dt.date.fromisoformat(value)
    return {'profile': profile, 'subjectSlugs': sorted(subjects), 'roomCodes': sorted(rooms),
            'holidayDates': sorted(holidays),
            'sources': {paths[key]: hashlib.sha256((root / paths[key]).read_bytes()).hexdigest()
                        for key in paths}}


def assess_catalogs(payloads, requirements):
    """Return only reference keys/aggregates; never copy account or business rows."""
    issues = []

    def rows(key, fields):
        value = payloads.get(key)
        if not isinstance(value, list) or any(not isinstance(r, dict) for r in value):
            issues.append(key + ': expected array of objects')
            return []
        if any(not all(field in r for field in fields) for r in value):
            issues.append(key + ': required response fields missing')
        ids = [r.get('id') for r in value]
        if any(not isinstance(i, int) or isinstance(i, bool) or i <= 0 for i in ids):
            issues.append(key + ': invalid IDs')
        elif len(ids) != len(set(ids)):
            issues.append(key + ': duplicate IDs')
        return value

    subjects = rows('subjects', ['id', 'slug', 'status'])
    rooms_value = rows('rooms', ['id', 'roomCode', 'status'])
    holidays = rows('holidays', ['id', 'holidayDate', 'active'])
    active = rows('activeHolidays', ['id', 'holidayDate', 'active'])
    classes = rows('classes', ['id', 'classType', 'status'])

    def keys(value, field, label):
        result = [r.get(field) for r in value]
        if any(not isinstance(k, str) or not k.strip() for k in result):
            issues.append(label + ': invalid catalog keys')
        valid = [k for k in result if isinstance(k, str) and k.strip()]
        if len(valid) != len(set(valid)):
            issues.append(label + ': duplicate catalog keys')
        return sorted(set(valid))

    subject_keys = keys(subjects, 'slug', 'subjects')
    room_keys = keys(rooms_value, 'roomCode', 'rooms')
    holiday_keys = keys(holidays, 'holidayDate', 'holidays')
    keys(active, 'holidayDate', 'activeHolidays')
    for row in holidays + active:
        try:
            dt.date.fromisoformat(row.get('holidayDate', ''))
        except (TypeError, ValueError):
            issues.append('holidays: invalid date')
        if not isinstance(row.get('active'), bool):
            issues.append('holidays: active must be boolean')
    # /active is a filter, never an oracle for whether the all-list is complete.
    expected_active = {r['id']: r for r in holidays if r.get('active') is True and type(r.get('id')) is int}
    actual_active = {r['id']: r for r in active if type(r.get('id')) is int}
    if any(r.get('active') is not True for r in active) or expected_active != actual_active:
        issues.append('holidays: all/active endpoints disagree; recheck concurrent catalog changes')
    missing = {}
    if requirements is not None:
        for label, field, actual in [('subjects', 'subjectSlugs', subject_keys),
                                      ('rooms', 'roomCodes', room_keys),
                                      ('holidays', 'holidayDates', holiday_keys)]:
            absent = sorted(set(requirements[field]) - set(actual))
            if absent:
                missing[label] = absent
    status = 'FAIL' if issues or missing else 'PASS' if requirements is not None else 'NOT_RUN'
    return {'status': status, 'profile': requirements['profile'] if requirements else None,
            'sourceRequirements': requirements,
            'observed': {'subjectSlugs': subject_keys, 'roomCodes': room_keys,
                         'holidayDates': holiday_keys,
                         'activeHolidayDates': sorted(r['holidayDate'] for r in active
                                                      if isinstance(r.get('holidayDate'), str)),
                         'inactiveHolidayCount': sum(r.get('active') is False for r in holidays),
                         'classCountByType': dict(Counter(str(r.get('classType')) for r in classes)),
                         'classCountByStatus': dict(Counter(str(r.get('status')) for r in classes))},
            'missingRequiredKeys': missing, 'issues': sorted(set(issues)),
            'businessFixtureReadiness': 'NOT_RUN',
            'limits': ['Reference key membership only; extras and authorized flag/price changes are allowed',
                       'All-list includes inactive holidays; active-list may legitimately be empty',
                       'No proof of whole DB completeness, migration markers, fixture window, paid rights or FE browser readiness',
                       'Missing profile means NOT_RUN, never a completeness PASS']}


def read_catalogs(request, alias, requirements):
    payloads = {}
    for key, path in [('subjects', '/api/subjects'), ('rooms', '/api/rooms'),
                      ('holidays', '/api/holidays'), ('activeHolidays', '/api/holidays/active'),
                      ('classes', '/api/classes')]:
        _, payloads[key], _ = request(alias, 'GET', path, expected=(200,))
    return assess_catalogs(payloads, requirements)


def assess_bundle_holidays(bundle, requirements):
    """Static gate for the fixture compiler's literal INSERT format; never execute SQL.

    An unknown SQL form is REVIEW_REQUIRED, not a false completeness PASS. This
    cannot certify transactions/triggers/anchor or replace the local PG reset test.
    """
    path = Path(bundle)
    sql = path.read_text(encoding='utf-8-sig')
    inserts = re.findall(r'\bINSERT\s+INTO\s+(?:public\.)?holidays\s*\(([^)]*)\)\s*([^;]*);',
                         sql, flags=re.IGNORECASE)
    dates = []
    unsupported = False
    for columns, body in inserts:
        names = [c.strip().lower() for c in columns.split(',')]
        if (names != ['holiday_date', 'holiday_name', 'tag', 'is_active']
                or not re.match(r'\s*VALUES\b', body, flags=re.IGNORECASE)):
            unsupported = True
            continue
        parsed = re.findall(r"\(\s*'(\d{4}-\d{2}-\d{2})'\s*,", body)
        dates.extend(parsed)
        if not parsed:
            unsupported = True
    missing = sorted(set(requirements['holidayDates']) - set(dates))
    return {'status': 'REVIEW_REQUIRED' if unsupported else 'FAIL' if missing else 'PASS',
            'profile': requirements['profile'], 'bundle': path.name,
            'bundleSha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'literalHolidayDates': sorted(set(dates)), 'missingHolidayDates': missing,
            'unsupportedHolidaySql': unsupported,
            'scope': 'Annual holiday membership in literal fixture INSERTs only; no SQL execution',
            'limits': ['Unknown INSERT form requires local executed SQL proof',
                       'Not an anchor/schema/reset/whole-data acceptance; never bypass the bundle date guard']}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Offline check of demo-bundle holiday coverage; no SQL execution')
    parser.add_argument('--check-bundle', required=True)
    parser.add_argument('--profile', choices=[PROFILE], required=True)
    parser.add_argument('--output')
    args = parser.parse_args()
    report = assess_bundle_holidays(args.check_bundle,
                                   load_requirements(Path(__file__).resolve().parents[1], args.profile))
    if args.output:
        target = Path(args.output); target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))
    raise SystemExit(0 if report['status'] == 'PASS' else 1)
