"""Merge fresh isolated HTTP/committed-state proof into the single QA record. Offline only."""
from pathlib import Path
import argparse
import datetime as dt
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--run', required=True, help='Raw run under artifacts/runs/')
p.add_argument('--checkout', required=True, help='Isolated checkout actually tested, without .env')
p.add_argument('--http-dir', default='postgres-http-final')
a = p.parse_args()
run = (ROOT / a.run).resolve()
run.relative_to((ROOT / 'artifacts/runs').resolve())
checkout = Path(a.checkout).resolve()
if (checkout / '.env').exists():
    raise SystemExit('Refusing a checkout containing .env')
test_path = Path('src/test/java/com/sportscenter/workflow/PhaseWorkflowHttpTest.java')
test_hash = hashlib.sha256((checkout / test_path).read_bytes()).hexdigest()
if test_hash != hashlib.sha256((ROOT / test_path).read_bytes()).hexdigest():
    raise SystemExit('Current HTTP test differs from the tested copy')
source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=checkout, text=True).strip()
for location in (ROOT, checkout):
    if subprocess.run(['git', 'diff', '--quiet', source, '--', 'src/main', 'pom.xml', 'database'], cwd=location).returncode:
        raise SystemExit('Current or isolated backend differs from the recorded source')

def load(path):
    # Windows PowerShell redirection can produce UTF-16; JSON detects the BOM.
    return json.loads(path.read_bytes())

def suite(path):
    return ET.parse(path).getroot()

def safe(value):
    if isinstance(value, str):
        return re.sub(r'vnp_SecureHash=[^&"\s]+', 'vnp_SecureHash=[redacted]', value)
    if isinstance(value, list):
        return [safe(v) for v in value]
    if isinstance(value, dict):
        return {k: safe(v) for k, v in value.items()}
    return value

def counts(suites):
    return {k: sum(int(s.get(k, 0)) for s in suites) for k in ('tests', 'failures', 'errors', 'skipped')}

clean_files = sorted((run / 'clean-xml').glob('TEST-*.xml'))
if not clean_files:
    raise SystemExit('Missing clean test reports')
clean = counts([suite(f) for f in clean_files])
pg = suite(run / 'postgres-workflow.xml')
pg_counts = counts([pg])
executed_hash = pg.find("./properties/property[@name='scms.test-source-sha256']")
if executed_hash is None or executed_hash.get('value') != test_hash:
    raise SystemExit('Executed XML source hash does not match the current/tested HTTP test file')
pg_url = pg.find("./properties/property[@name='scms.phases.pg-url']")
if pg_url is None or not re.fullmatch(r'jdbc:postgresql://127\.0\.0\.1:55440/scms_phases_[a-z0-9_]+', pg_url.get('value', '')):
    raise SystemExit('Missing guarded local PostgreSQL target in executed XML')
prepare_path = run / 'postgres-prepare-final.log'
if not prepare_path.exists():
    prepare_path = run / 'postgres-prepare.log'
migration = load(prepare_path)
if not migration.get('localOnly') or not pg_url.get('value').endswith('/'+migration['database']):
    raise SystemExit('Migration and executed PostgreSQL target do not match')
if any(clean[k] or pg_counts[k] for k in ('failures', 'errors')) or pg_counts['skipped']:
    raise SystemExit('Acceptance incomplete: inspect failures/setup/skips before recording PASS')
outcomes = {t.get('name'): 'FAIL' if t.find('failure') is not None or t.find('error') is not None
            else 'BLOCKED' if t.find('skipped') is not None else 'PASS' for t in pg.findall('testcase')}
raw = run / a.http_dir
exchanges = safe(load(raw / 'http-exchanges.json'))
for e in exchanges:
    if e.get('expectedStatus') == 0:
        e['expectedStatus'] = None
        e['statusValidation'] = 'Concurrent result set asserted by the scenario; see acceptance expected'
cases = load(raw / 'acceptance-cases.json')
actor_rows = load(raw / 'actors.json')
fixture_roles = {
    'phase.member': 'Member', 'phase.other': 'Member', 'phase.member2': 'Member',
    'phase.coach': 'Coach', 'phase.unassigned': 'Coach', 'phase.coach2': 'Coach', 'phase.multisubject': 'Coach',
    'phase.manager': 'CenterManager', 'phase.manager2': 'CenterManager',
    'phase.cashier': 'Receptionist', 'phase.cashier2': 'Receptionist', 'phase.noauthority': 'Admin',
}
fixture_actors = {row['full_name']: row for row in actor_rows}
session_checks = 0
for e in exchanges:
    if e['path'] != '/api/auth/me':
        continue
    me = e['responseBody']
    prefix = me['email'].removesuffix('@example.test')
    expected_role = fixture_roles[prefix]
    required = {'ROLE_' + expected_role.upper()}
    if expected_role == 'CenterManager':
        required |= {'VIEW_REPORTS', 'MANAGE_CLASSES', 'MANAGE_INVOICES', 'MANAGE_SUBSCRIPTIONS'}
    if expected_role == 'Receptionist':
        required.add('MANAGE_INVOICES')
    if expected_role == 'Coach':
        required.add('RECORD_RESULT')
    if prefix == 'phase.unassigned':
        required.add('VIEW_REPORTS')
    if e['actualStatus'] != 200 or me['userId'] != fixture_actors[prefix]['user_id'] or me['role'] != expected_role or not required.issubset(me['authorities']):
        raise SystemExit('Session identity/role/authority does not match independent actor fixture: ' + prefix)
    if expected_role == 'Member' and me['memberId'] != fixture_actors[prefix]['user_id']:
        raise SystemExit('Member profile identity mismatch: ' + prefix)
    if prefix == 'phase.noauthority' and 'VIEW_REPORTS' in me['authorities']:
        raise SystemExit('No-authority fixture unexpectedly has VIEW_REPORTS')
    session_checks += 1
for prefix, row in fixture_actors.items():
    row['expectedRole'] = fixture_roles[prefix]
environment = 'LOCAL_POSTGRESQL_17_HTTP_ISOLATED_LOOPBACK'
for c in cases:
    selected = [exchanges[i] for i in c['exchangeIndexes']]
    if not selected or not c['stateAfterCommit']:
        raise SystemExit('Missing response/state evidence: ' + c['id'])
    c.update(status=outcomes[c['scenario']], environment=environment, sourceCommit=source,
             actors=sorted({e['actor'] for e in selected if e['path'] != '/api/auth/me'}),
             apis=sorted({e['method'] + ' ' + e['path'].split('?')[0] for e in selected if e['path'] != '/api/auth/me'}))
    # Store each HTTP body once in local.exchanges; cases refer to its zero-based index.
    c['responseEvidence'] = [{'exchangeIndex': i, 'httpStatus': exchanges[i]['actualStatus']}
                             for i in c['exchangeIndexes']]
if len({c['id'] for c in cases}) != len(cases):
    raise SystemExit('Duplicate acceptance case id')
if any(c['status'] != 'PASS' for c in cases):
    raise SystemExit('Do not publish failed scenarios as accepted')

scoped_decisions = ['D02', 'D03', 'D04', 'D05', 'D06', 'D07', 'D08', 'D10', 'D12', 'D13', 'D14', 'D15', 'D16', 'D17', 'D18']
gaps = [
    ('RENDER-AUTHENTICATED', 'BLOCKED', scoped_decisions, 'RENDER_NOT_PROBED',
     'Same API flows as local cases', 'Approved test accounts for Member/Coach/Receptionist/Manager in two centers, fixtures, mutation scope and post-commit read access are missing',
     'Verify /api/auth/me, then response and committed state for the approved role/owner/center matrix'),
    ('VNPAY-GATEWAY-LIVE', 'BLOCKED', ['D03', 'D10'], 'VNPAY_SANDBOX_NOT_CONTACTED',
     'VNPay checkout/Return/IPN', 'Merchant, callback fixture and live test scope missing',
     'Real gateway callback produces one Paid activation or reconciliation at cutoff'),
    ('BASELINE-CRUD-ROLE-MATRIX', 'NOT_RUN', ['D19'], environment,
     'CRUD users/staff/roles/permissions/rooms/packages/training-plans/evaluations/feedback outside the recorded workflow operations',
     'This matrix prioritizes D02-D18 workflows; fixtures do not cover every baseline CRUD mutation and role/owner/center combination',
     'Independently establish each baseline contract and authorized actor fixture, then verify response and committed state; route presence grants no business PASS'),
    ('SUBSTITUTE-COACH-HTTP', 'NOT_RUN', ['D06'], environment,
     'POST /api/sessions/{id}/substitute-coach', 'Positive substitute Coach fixture qualified for the same subject has not been exercised in this HTTP matrix',
     'Manager with incident and exact-plan consent changes only session Coach, preserves Primary Coach and credit; rejects wrong subject/center/overlap'),
    ('HOLIDAY-CROSS-CENTER-HTTP', 'NOT_RUN', ['D12', 'D14'], environment,
     'POST /api/holidays/preview; POST /api/holidays; PATCH /api/holidays/{id}/toggle',
     'No second-center entitled subscription/session affected by a global Holiday in this matrix',
     'Preview/confirm cannot silently mutate another center; unresolved impacts leave all entitlements unchanged'),
    ('OUTDOOR-ADMISSION-HTTP', 'NOT_RUN', ['D03', 'D04', 'D12'], environment,
     'Advanced registration/order; POST /api/sessions/book-private',
     'HTTP fixtures cover Indoor/one subject; Outdoor slots 3/4 and multi-subject admission saturation need separate scenarios',
     'Outdoor slots 3/4 rejected; admission recomputed under lock across subjects without promising reserved sessions'),
]
closed_by = {
    'SUBSTITUTE-COACH-HTTP': {'CENTER-SUBSTITUTE-CONSENT'},
    'HOLIDAY-CROSS-CENTER-HTTP': {'HOLIDAY-OTHER-CENTER'},
    'OUTDOOR-ADMISSION-HTTP': {'ADMISSION-MULTISUBJECT', 'OUTDOOR-HEAT-HTTP'},
}
completed = {c['id'] for c in cases}
gaps = [g for g in gaps if g[0] not in closed_by or not closed_by[g[0]].issubset(completed)]
remaining = [dict(id=i, status=s, decisions=d, environment=e, apis=[api], actors=['Required role/owner/center actors'],
                  fixture=f, expected=x, responseEvidence=None, stateAfterCommit=None,
                  sourceCommit=source if e == environment else None) for i,s,d,e,api,f,x in gaps]
qa_path = ROOT / 'artifacts/qa/latest.json'
qa = load(qa_path)
now = dt.datetime.now(dt.timezone.utc).isoformat()
qa['recordedAt'] = now
qa['purpose'] = 'FE handoff readiness: fresh local authenticated HTTP and post-commit acceptance; retained cloud evidence is not rerun'
qa['results'].update(result='PARTIAL_LOCAL_PASS_LIVE_BLOCKED', sourceCommit=source,
                     cleanTests=clean['tests'], cleanPassed=clean['tests']-clean['skipped'], cleanSkipped=clean['skipped'],
                     postgresTests=pg_counts['tests'], postgresBusinessWorkflows=pg_counts['tests']-1,
                     postgresContractRegressions=1, recordedExchanges=len(exchanges),
                     authenticatedBusinessOperationsExecutedOnRender=0, productionMutationTestsExecuted=0)
qa['local'].update(workflowCases=list(outcomes), exchanges=exchanges,
                   openApiRegression=load(raw / 'openapi-contract-check.json'), rawRunOnThisMachine=run.relative_to(ROOT).as_posix())
qa['authenticatedAcceptance'] = dict(
    recordedAtUtc=now, sourceCommit=source, backendChanged=False,
    testedWorkingTreeFiles={test_path.as_posix(): test_hash},
    environment=environment, clean=clean, postgres=pg_counts,
    databaseUrl=pg_url.get('value'), migrations=migration,
    actors=actor_rows, cases=cases, remaining=remaining,
    caseStatusCounts={s: sum(c['status'] == s for c in cases+remaining) for s in ('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN')},
    sessionVerification='GET /api/auth/me validates identity/role/authorities after each login and the five baseline actors before every test group',
    sessionFixtureValidation=dict(result='PASS', verifiedResponses=session_checks,
        method='Recorded auth/me userId/memberId compared with committed actor rows; role and required authorities compared with explicit fixture roles, not merely with login DTO'),
    stateVerification='Response completed before separate JdbcTemplate read; request transactions commit independently; assertions and SQL snapshots retained',
    raceMethod='Two ready worker threads released by one latch, real concurrent HTTP requests and PostgreSQL; finite fixture proves observed invariants, not every interleaving',
    excluded=['P1.5', 'package T', 'D01', 'D09', 'FE integration'],
    readiness='Contract/schema available for scoped BE handoff; full Render authenticated/FE acceptance remains open',
    setupAttempts=[dict(status='BLOCKED', reason='Initial new center fixture omitted required center_name; dependent workflow results not accepted'),
                   dict(status='BLOCKED', reason='Next last-seat fixture sent a non-UUID requestId; corrected fixture without changing expected business outcomes')],
    limits=['No cloud .env read, historical seed account, production login/mutation, browser FE or real VNPay gateway used',
            'Unit/H2 regression skips are not PASS; historical cloud/communication/clone records retain their original SHA and dates',
            'API errors/formats/nullability/ranges and every implemented baseline CRUD authorization combination are not exhaustively validated'])
qa['ciAndDeployment']['evidenceKind'] = 'RETAINED_NOT_RERUN_BY_AUTHENTICATED_ACCEPTANCE'
candidate = run / 'qa-candidate.json'
candidate.write_text(json.dumps(qa, ensure_ascii=False, indent=2)+'\n', encoding='utf-8', newline='\n')
audit = subprocess.run(['python', str(ROOT / 'scripts/audit-fe-contract.py'), '--qa', str(candidate)],
                       cwd=ROOT, text=True, capture_output=True)
if audit.returncode:
    raise SystemExit('Offline contract audit failed; canonical QA unchanged: ' + audit.stdout + audit.stderr)
wire = json.loads(audit.stdout)
qa['wireCompatibility'].update(result=wire['result'], checkedAtUtc=wire['checkedAtUtc'],
    method='Fresh local successful HTTP exchanges compared with the canonical OpenAPI snapshot; no live schema fetch',
    localExchangeCount=len(exchanges), positiveExchangesChecked=wire['positiveRecordedExchanges'],
    requestResponseBodyChecks=wire['bodyTypePropertyChecks'], uniqueIssues=len(wire['wireIssues']))
qa['results'].update(positiveExchangesChecked=wire['positiveRecordedExchanges'],
                    bodyCompatibilityChecks=wire['bodyTypePropertyChecks'], wireIssues=len(wire['wireIssues']))
spec = load(ROOT / 'artifacts/fe-handoff/openapi.json')
covered = set()
for e in exchanges:
    for path, item in spec['paths'].items():
        if e['method'].lower() in item and re.fullmatch(re.sub(r'\{[^}]+\}', '[^/]+', path), e['path'].split('?')[0]):
            covered.add((e['method'], path))
qa['results']['operationsWithLocalExchanges'] = len(covered)
qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2)+'\n', encoding='utf-8', newline='\n')
candidate.unlink()
print(json.dumps(dict(sourceCommit=source, clean=clean, postgres=pg_counts,
                      cases=qa['authenticatedAcceptance']['caseStatusCounts'], qa=qa_path.relative_to(ROOT).as_posix())))
