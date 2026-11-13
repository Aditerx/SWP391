"""Shared dates and preflight for synthetic demo generation; never accesses a DB."""
from datetime import date, datetime, timedelta
import re
from zoneinfo import ZoneInfo

TIMEZONE = 'Asia/Ho_Chi_Minh'
VN = ZoneInfo(TIMEZONE)
FIXTURE_VERSION = 'normal-phase1-v3'


def anchor_date(value):
    if not re.fullmatch(r'\d{4}-\d{2}-\d{2}', value):
        raise ValueError('Anchor must be an explicit YYYY-MM-DD Vietnam date')
    return date.fromisoformat(value)


def demo_time(value, anchor):
    at = datetime.fromisoformat(value)
    if at.tzinfo is None or at.utcoffset() is None:
        raise ValueError('Demo time must include an offset, for example T10:00:00+07:00')
    at = at.astimezone(VN)
    if at.date() < anchor:
        raise ValueError('Demo time cannot precede the bundle installation anchor')
    return at


def sql_anchor_guard(anchor):
    # Wall time is intentional: CURRENT_DATE is frozen at transaction start.
    return f"""DO $anchor$
DECLARE business_date DATE := (clock_timestamp() AT TIME ZONE '{TIMEZONE}')::date;
BEGIN
 IF business_date<>DATE '{anchor.isoformat()}' THEN
  RAISE EXCEPTION 'SCMS_DEMO_STALE_ANCHOR: bundle anchor %, current VN date %; regenerate and verify the whole bundle', DATE '{anchor.isoformat()}', business_date;
 END IF;
END $anchor$;
"""


def scenario_plan(tables, anchor, at=None):
    """Describe availability, never certify that a workflow was executed."""
    classes = {c['key']: c for c in tables['classes'] if c['type'] == 'Normal'}
    withdrawn = {r['enrollment'] for r in tables['payment_resolutions'] if r['type'] == 'Refund'}
    courses = []
    for key, c in classes.items():
        first = datetime.fromisoformat(c['first_session']).replace(tzinfo=VN)
        registered = sum(e['sports_class'] == key and e['status'] == 'Registered' and e['key'] not in withdrawn
                         for e in tables['class_enrollments'])
        courses.append({'classKey': key, 'name': c['name'], 'state': c['status'],
                        'firstSession': first.isoformat(),
                        'registrationClosesAt': (first-timedelta(hours=48)).isoformat(),
                        'confirmationReasonRequiredFrom': (first-timedelta(hours=24)).isoformat(),
                        'minCapacity': c['min_capacity'], 'maxCapacity': c['max_capacity'],
                        'paidRegisteredBeforeDemo': registered})
    windows = {c['classKey']: c for c in courses}

    def case(key, actors, fixtures, start, end, expected):
        availability = 'NOT_EVALUATED'
        if at is not None:
            available = (start is None or at >= start) and (end is None or at < end)
            availability = 'AVAILABLE' if available else 'OUTSIDE_WINDOW'
        return {'case': key, 'actors': actors, 'fixtures': fixtures,
                'availability': availability,
                'executionStatus': 'NOT_RUN',
                'availableFromInclusive': start.isoformat() if start else None,
                'availableUntilExclusive': end.isoformat() if end else None,
                'expected': expected}

    def first(key):
        return datetime.fromisoformat(windows[key]['firstSession'])

    cases = [
        case('DEMO-01_READ_SCOPE', ['ADM', 'CM1', 'R1', 'PICK-A', 'M02'], ['catalog', 'roster', 'refunds'],
             None, None, 'Fresh login/auth-me; owner/assignment/financial scope checks'),
        case('DEMO-02_CASH_N01', ['R1', 'M14', 'M15'], ['N01'], None, first('N01')-timedelta(hours=48),
             'Two counter Cash payments900000; Paid+Registered; retry creates no invoice/right'),
        case('DEMO-03_CONFIRM_N01', ['CM1'], ['N01', 'DEMO-02_CASH_N01'], None, first('N01'),
             'After Cash: Paid Registered3, minimum3; persist confirmation once'),
        case('DEMO-04_CUTOFF_N02', ['R1', 'M14'], ['N02'], first('N02')-timedelta(hours=48), first('N02'),
             'Registration denied NORMAL_REGISTRATION_CLOSED; no Invoice/enrollment'),
        case('DEMO-05_LATE_CONFIRM_N02', ['CM1'], ['N02'], first('N02')-timedelta(hours=24), first('N02'),
             'Missing reason denied; DEMO reason permits confirmation if all conditions hold'),
        case('DEMO-06_INSUFFICIENT_N03', ['CM1'], ['N03'], None, first('N03'),
             'Paid Registered6 below minimum7; confirmation denied'),
        case('DEMO-07_ONGOING_CANCEL_DENIED', ['CM1'], ['N08'], None, None,
             'Ongoing cannot cancel-before-opening; state/history unchanged'),
        case('DEMO-08_TRANSFER', ['M01', 'M03', 'PICK-A', 'PICK-B'], ['N05', 'N11', 'EV-N05'],
             None, min(first('N05'), first('N11')-timedelta(hours=48)),
             'M01 Accept consumes last N11 seat; original Invoice preserved; M03 remains Waiting when full'),
        case('DEMO-09_REFUND_DEMO', ['M04', 'CM1', 'R1'], ['N06', 'RF02'], None, None,
             'Existing obligation: receiver→verify→approve→claim→proof→independent DEMO verification; no real transfer'),
        case('DEMO-10_REPORT_RELOAD', ['CM1', 'M01', 'M04'], ['DEMO-02_CASH_N01', 'DEMO-08_TRANSFER', 'DEMO-09_REFUND_DEMO'],
             None, None, 'Reload persisted state and verify separate receipt/refund periods; upload alone is not Completed')]
    actors = [{'key': u['key'], 'name': u['name'], 'role': u['role'], 'status': u['status']}
              for u in tables['users'] if u['role'] == 'Admin' or u['key'] in
              {'CM1', 'R1', 'PICK-A', 'PICK-B', 'BAD-A', 'M01', 'M02', 'M03', 'M04', 'M14', 'M15'}]
    return {'fixtureVersion': FIXTURE_VERSION, 'anchorDate': anchor.isoformat(),
            'timeZone': TIMEZONE, 'plannedDemoAt': at.isoformat() if at is not None else None,
            'dataState': 'EXPECTED_FRESH_FIXTURE_NOT_CLOUD_STATE',
            'note': 'Availability describes time only; recheck runtime actor/state/version/seat before writing. FE suggestions require FE review.',
            'actors': actors, 'classes': courses, 'cases': cases}
