"""Shared dates and preflight for synthetic demo generation; never accesses a DB."""
from datetime import date, datetime, timedelta
import re
from zoneinfo import ZoneInfo

TIMEZONE = 'Asia/Ho_Chi_Minh'
VN = ZoneInfo(TIMEZONE)
FIXTURE_VERSION = 'demo-20261119-v4'

SLOTS = {1: ('06:00', '07:30'), 2: ('07:45', '09:15'), 3: ('09:30', '11:00'),
         4: ('13:00', '14:30'), 5: ('14:45', '16:15'), 6: ('16:30', '18:00'), 7: ('18:15', '19:45')}


def choose_session(tables, earliest, subject, coach, member, room, *, expires=None, weekday=None, reserved=()):
    """Choose a valid future slot against the full design, without changing historical rows."""
    work={(r['coach'],r['slot_key']) for r in tables['coach_work_slots']}
    off={(r['coach'],r['date']) for r in tables['coach_days_off']}
    holidays={r['date'] for r in tables['holidays'] if r['is_active']}
    outdoor=next(r['outdoor'] for r in tables['subjects'] if r['key']==subject)
    participants={c['key']:{e['member'] for e in tables['class_enrollments']
                           if e['sports_class']==c['key'] and e['status']=='Registered'} for c in tables['classes']}
    for delta in range(45):
        day=earliest+timedelta(days=delta)
        if weekday is not None and day.isoweekday()!=weekday:continue
        if day.isoformat() in holidays or (coach,day.isoformat()) in off:continue
        for number,(start,end) in SLOTS.items():
            if outdoor and number in (3,4):continue
            if (coach,day.strftime('%A').upper()+':'+str(number)) not in work:continue
            if expires is not None and datetime.fromisoformat(day.isoformat()+'T'+end)>expires:continue
            candidate={'date':day.isoformat(),'start':start,'end':end,'coach':coach,'member':member,'room':room}
            conflict=False
            for s in [*tables['sessions'],*reserved]:
                if s.get('status')=='Cancelled' or s.get('center_invalidated'):continue
                if s['date']!=candidate['date'] or s['start']>=end or s['end']<=start:continue
                members={s['member']} if s.get('member') else participants.get(s.get('sports_class'),set())
                if s['coach']==coach or s['room']==room or member in members:
                    conflict=True;break
            if not conflict:return candidate
    raise ValueError('DEMO_NO_VALID_SLOT: '+subject+'/'+coach+'/'+member)


def holiday_catalog(sql):
    """Read the approved inactive catalog; reject partial/unknown SQL shapes."""
    start = 'WITH catalog(holiday_date, holiday_name, tag) AS ('
    if sql.count(start) != 1 or sql.count('), inserted AS') != 1:
        raise ValueError('DEMO_HOLIDAY_CATALOG_INVALID: expected one catalog block')
    block = sql.split(start, 1)[1].split('), inserted AS', 1)[0].strip()
    if not block.startswith('VALUES'):
        raise ValueError('DEMO_HOLIDAY_CATALOG_INVALID: literal VALUES required')
    block = block[len('VALUES'):]
    pattern = r"\(\s*DATE '(\d{4}-\d{2}-\d{2})'\s*,\s*'((?:[^']|'')*)'\s*,\s*'([A-Z_]+)'\s*\)"
    matches = re.findall(pattern, block)
    if not matches or re.sub(pattern, '', block).strip(' \t\r\n,'):
        raise ValueError('DEMO_HOLIDAY_CATALOG_INVALID: unparsed catalog rows')
    rows = [{'date': date.fromisoformat(day).isoformat(), 'name': name.replace("''", "'"),
             'tag': tag, 'is_active': False} for day, name, tag in matches]
    if len({r['date'] for r in rows}) != len(rows):
        raise ValueError('DEMO_HOLIDAY_CATALOG_INVALID: duplicate dates')
    return rows


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


def scenario_plan(tables, anchor, at=None, until=None):
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
    # Fresh operations happen during the presentation; these dates are planned inputs, not seeded purchases.
    reserved=[];normal=[];cursor=anchor+timedelta(days=7)
    holidays={h['date'] for h in tables['holidays'] if h['is_active']}
    while len(normal)<12:
        if cursor.isoweekday() in (1,3,5) and cursor.isoformat() not in holidays:
            s=choose_session(tables,cursor,'BAD','BAD-A','M14','C01-BAD-01',reserved=reserved)
            if s['date']!=cursor.isoformat() or s['start']!='06:00':
                raise ValueError('DEMO_MAIN_NORMAL_RESOURCE_CONFLICT: '+cursor.isoformat())
            normal.append(s);reserved.append(s)
        cursor+=timedelta(days=1)
    advanced=choose_session(tables,anchor+timedelta(days=5),'SWIM','SWIM-A','M14','C01-SWIM-01',
                            expires=datetime.combine(anchor+timedelta(days=45),datetime.min.time()),reserved=reserved)
    sunday=choose_session(tables,anchor+timedelta(days=5),'SWIM','SWIM-A','M14','C01-SWIM-01',weekday=7,
                         expires=datetime.combine(anchor+timedelta(days=45),datetime.min.time()),reserved=[*reserved,advanced])
    refund=next(s for s in tables['sessions'] if s['key']=='SUB01-S01')
    def start_of(s):return datetime.fromisoformat(s['date']+'T'+s['start']).replace(tzinfo=VN)
    limits={'normalRegistration':start_of(normal[0])-timedelta(hours=48),
            'normalOpening':start_of(normal[0]),'advancedAcceptInclusive':start_of(advanced)-timedelta(hours=12),
            'refundCancellationInclusive':start_of(refund)-timedelta(hours=12)}
    checks=[]
    for moment in [x for x in [at,until] if x is not None]:
        checks.append({'at':moment.isoformat(),'status':'PASS' if all(moment<d for d in limits.values()) else 'FAIL'})
    main={'normal':{'operation':'Create Normal during demo; min=max=1, buy Cash, confirm, initial attendance',
                    'subject':'BAD','coach':'BAD-A','member':'M14','room':'C01-BAD-01',
                    'daysOfWeek':[1,3,5],'startDate':normal[0]['date'],'sessions':normal},
          'advanced':{'operation':'AR01 Requested: Coach accepts during demo; Member orders, Cash Paid, proposal and Member Accept',
                      'registration':'AR01','subject':'SWIM','session':advanced},
          'refund':{'session':'SUB01-S01','subscription':'SUB01','proposal':'DEMO-CANCEL-SUB01',
                    'beforeCredit':11,'afterCredit':12,'expiryUnchanged':True,'schedule':refund},
          'sunday':{'operation':'Optional separate proposal/Accept on Sunday; check availability again', 'session':sunday},
          'limits':{k:v.isoformat() for k,v in limits.items()},'windowChecks':checks,
          'relativeDeadlines':'New invoice<=30min; issued attendance code300seconds; Advanced order<acceptedAt+24h; rights45days from valid Paid. Issue during demo; do not extend.'}
    return {'fixtureVersion': FIXTURE_VERSION, 'anchorDate': anchor.isoformat(),
            'timeZone': TIMEZONE, 'plannedDemoAt': at.isoformat() if at is not None else None,
            'plannedDemoUntil':until.isoformat() if until else None,'mainPaths':main,
            'dataState': 'EXPECTED_FRESH_FIXTURE_NOT_CLOUD_STATE',
            'note': 'Availability describes time only; recheck runtime actor/state/version/seat before writing. FE suggestions require FE review.',
            'actors': actors, 'classes': courses, 'cases': cases}
