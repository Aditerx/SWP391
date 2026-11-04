"""Run the complete BE suite and QueryDR PostgreSQL HTTP proof in a copy without .env."""
from pathlib import Path
import datetime as dt
import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from qa_fingerprint import source_fingerprints

root = Path(__file__).resolve().parents[1]
output = root / 'artifacts/runs/querydr'
output.mkdir(parents=True, exist_ok=True)
migration = json.loads((output / 'migration.json').read_text(encoding='utf-8'))
assert migration['result'] == 'PASS'
database = migration['behaviorDatabase']
assert re.fullmatch(r'scms_querydr_[a-z0-9_]+', database)
assert migration['migrationSha256'] == hashlib.sha256((root / 'database/migrations/15_QueryDR.sql').read_bytes()).hexdigest()
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--resume-sandbox', help='Reuse the same verified source copy after an infrastructure-only PG failure')
parser.add_argument('--retain-email-snapshot', action='store_true', help='Keep the already tested snapshot; report concurrent email edits separately')
parser.add_argument('--refresh-query-fixture', action='store_true', help='Retest the QueryDR fixture on H2/PG after a fixture-only adjustment; keep baseline clean evidence')
args = parser.parse_args()
if args.resume_sandbox:
    sandbox = Path(args.resume_sandbox).resolve()
    assert sandbox.parent == Path(tempfile.gettempdir()).resolve() and sandbox.name.startswith('scms_querydr_local_')
else:
    sandbox = Path(tempfile.mkdtemp(prefix='scms_querydr_local_'))
    for folder in ['src', 'database', '.mvn', 'scripts']:
        shutil.copytree(root / folder, sandbox / folder, ignore=shutil.ignore_patterns('__pycache__'))
    for name in ['pom.xml', 'mvnw', 'mvnw.cmd', 'schema_postgres.sql']:
        shutil.copy2(root / name, sandbox / name)
    (sandbox / 'artifacts/fe-handoff').mkdir(parents=True)
assert not (sandbox / '.env').exists()
if args.refresh_query_fixture:
    assert args.resume_sandbox
    fixture='src/test/java/com/sportscenter/invoice/VnPayQueryIntegrationTest.java'
    shutil.copy2(root/fixture,sandbox/fixture)
fingerprints = source_fingerprints(root)
copied_fingerprints = source_fingerprints(sandbox)
allowed_concurrent = {'src/main/java/com/sportscenter/auth/EmailService.java',
                      'src/test/java/com/sportscenter/auth/EmailServiceConfigurationTest.java',
                      'src/main/resources/application.properties'} if args.retain_email_snapshot else set()
concurrent = []


def implementation_unchanged():
    differences = []
    for folder in ['src/main', 'src/test']:
        names = {p.relative_to(root).as_posix() for p in (root / folder).rglob('*') if p.is_file()}
        names |= {p.relative_to(sandbox).as_posix() for p in (sandbox / folder).rglob('*') if p.is_file()}
        for name in names:
            if not (root/name).exists() or not (sandbox/name).exists() or (root/name).read_bytes() != (sandbox/name).read_bytes():
                assert name in allowed_concurrent, name
                differences.append(name)
    for name in ['pom.xml','schema_postgres.sql','scripts/test-room-absence-postgres.py','scripts/test-phases-postgres.py','scripts/prepare-phases-release.py']:
        assert (root/name).read_bytes() == (sandbox/name).read_bytes(), name
    properties = 'src/main/resources/application.properties'
    def query_config(base):
        return [line for line in (base/properties).read_text(encoding='utf-8').splitlines() if line.startswith(('vnpay.','payment.'))]
    assert query_config(root) == query_config(sandbox)
    # README may be edited during handoff; all actual DB scripts must remain exactly the tested copy.
    for source in (root / 'database').rglob('*'):
        if source.is_file() and source.suffix.lower() != '.md':
            assert source.read_bytes() == (sandbox / source.relative_to(root)).read_bytes(), str(source)
    concurrent[:] = sorted(differences)
    return source_fingerprints(sandbox)


def summary():
    cases = []
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    for path in (sandbox / 'target/surefire-reports').glob('TEST-*.xml'):
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, 0))
        if suite.attrib['name'].endswith(('VnPayQueryIntegrationTest', 'VnPayQueryClientTest')):
            cases += [case.attrib['classname'] + '.' + case.attrib['name'] for case in suite.findall('testcase')]
    return dict(totals, passed=totals['tests']-totals['failures']-totals['errors']-totals['skipped'], queryCases=cases)


def run(label, args):
    with (output / (label + '.log')).open('wb') as log:
        result = subprocess.run(['cmd.exe', '/d', '/c', 'mvnw.cmd', *args], cwd=sandbox, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(label + ' failed; inspect its local log')
    report = summary()
    assert report['tests'] > 0 and not report['failures'] and not report['errors'], report
    print(json.dumps({'stage': label, **report}), flush=True)
    return report


implementation_unchanged()
if args.resume_sandbox:
    log = (output / 'isolated-clean-test.log').read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESS' in log
    values = re.findall(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)', log)[-1]
    clean = dict(zip(('tests','failures','errors','skipped'), map(int, values)))
    assert clean['tests'] > 0 and clean['failures'] == clean['errors'] == 0
    clean['passed'] = clean['tests']-clean['skipped']
else:
    clean = run('isolated-clean-test', ['clean', 'test'])
targeted_h2 = run('final-h2-http', ['-Dtest=VnPayQueryIntegrationTest', 'test']) if args.refresh_query_fixture else None
if targeted_h2:
    clean['fixtureAdjustment']='Baseline full suite passed before center-name fixture adaptation; final fixture independently rerun on H2 and PG; production source unchanged'
# Remove only XML reports produced in this newly created private sandbox, not workspace data.
for path in (sandbox / 'target/surefire-reports').glob('TEST-*.xml'):
    path.unlink()
postgres = run('postgres-http', ['-Duser.timezone=Asia/Ho_Chi_Minh', '-Dtest=VnPayQueryIntegrationTest', '-Dscms.query.export-openapi=true',
                                '-Dscms.query.pg-url=jdbc:postgresql://127.0.0.1:55440/' + database, 'test'])
assert postgres['skipped'] == 0
spec = json.loads((sandbox / 'artifacts/fe-handoff/openapi.json').read_text(encoding='utf-8'))
endpoint = spec['paths']['/api/members/me/invoices/{id}/verify-vnpay']['post']
assert endpoint['requestBody']['content']['application/json']['schema']['type'] == 'object'
spec['servers'] = [{'url': 'https://scms-app-p1gy.onrender.com', 'description': 'Canonical FE server; schema generated locally'}]
(root / 'artifacts/fe-handoff/openapi.json').write_text(json.dumps(spec, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
fingerprints = implementation_unchanged()
report = {'result': 'PASS_LOCAL', 'checkedAtUtc': dt.datetime.now(dt.timezone.utc).isoformat(),
          'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
          'workingTree': True, 'sourceFingerprints': fingerprints, 'testSandbox': str(sandbox),
          'concurrentUntestedSourceFiles': concurrent,
          'envFilePresent': False, 'validation': {'cleanTest': clean, 'postgresHttp': postgres, 'migration': migration},
          'targetedH2AfterFixtureAdjustment': targeted_h2,
          'provider': 'Loopback HTTP protocol simulator; not live VNPay', 'cloudMutations': 0, 'liveGatewayCalls': 0}
(output / 'validation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'result': report['result'], 'clean': clean['passed'], 'skip': clean['skipped'], 'postgres': postgres['passed']}))
