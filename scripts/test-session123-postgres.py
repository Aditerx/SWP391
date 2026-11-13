"""Run Session1-3 HTTP/commit/race regressions only on disposable loopback PG.

No .env or canonical demo bundle. The existing minimal regression provisioner
keeps historical test Clocks separate from today's runtime/demo preconditions.
Each suite gets a fresh database and a source copy without runtime credentials.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from qa_fingerprint import source_fingerprints

SUITES = [
    ('content', 'TrainingResultContentIntegrationTest', 'scms.content.pg-url'),
    ('scope', 'TrainingResultScopeIntegrationTest', 'scms.result-scope.pg-url'),
    ('room', 'RoomValidationIntegrationTest', 'scms.room-validation.pg-url'),
    ('dates', 'AttendanceDateFilterIntegrationTest', 'scms.attendance-date.pg-url'),
    ('result', 'TrainingResultContentAfterSessionIntegrationTest', 'scms.result-content.pg-url'),
    ('history', 'TrainingPlanHistoryIntegrationTest', 'scms.plan-history.pg-url'),
    ('lastadmin', 'LastAdministratorIntegrationTest', 'scms.last-admin.pg-url'),
    ('rbac', 'RbacGuardIntegrationTest', 'scms.rbac.pg-url'),
    ('normal', 'NormalLifecycleHttpTest', 'scms.normal.pg-url'),
    ('workflow', 'PhaseWorkflowHttpTest', 'scms.phases.pg-url'),
    ('notification', 'NotificationOwnershipIntegrationTest', 'scms.notification.pg-url'),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--suffix', required=True, help='Unique new database suffix [a-z0-9_]')
    parser.add_argument('--output-dir', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z0-9_]{1,30}', args.suffix): parser.error('Invalid suffix')
    root = Path(__file__).resolve().parents[1]
    out = Path(args.output_dir).resolve()
    out.mkdir(parents=True, exist_ok=True)
    dest = out / 'postgres-source'
    if dest.exists(): raise ValueError('Use a new output directory; old evidence is retained')
    container = 'scms-phases-20261027'
    inspection = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
    assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [
        {'HostIp': '127.0.0.1', 'HostPort': '55440'}], 'Disposable loopback target required'
    template = 'scms_phases_s123_' + args.suffix
    with (out / 'provision.log').open('w', encoding='utf-8') as log:
        subprocess.run([sys.executable, str(root / 'scripts/test-phases-postgres.py'),
                        '--database', template, '--minimal-fixture'],
                       cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)
    props = []
    for alias, _, prop in SUITES:
        db = 'scms_phases_' + ('normal_' if alias == 'normal' else '') + 's123_' + args.suffix + '_' + alias
        sql = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-v',
                              'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'postgres'],
                             input='CREATE DATABASE ' + db + ' TEMPLATE ' + template + ';',
                             text=True, capture_output=True)
        if sql.returncode: raise RuntimeError('New local database provisioning failed: ' + alias)
        props.append('-D' + prop + '=jdbc:postgresql://127.0.0.1:55440/' + db)
    for folder in ('src', '.mvn'): shutil.copytree(root / folder, dest / folder)
    for name in ('pom.xml', 'mvnw', 'mvnw.cmd'): shutil.copy2(root / name, dest / name)
    assert not (dest / '.env').exists()
    command = [str(dest / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')),
               '-Dtest=' + ','.join(s[1] for s in SUITES),
               '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh',
               '-Dscms.room-validation.openapi=' + str(root / 'artifacts/fe-handoff/openapi.json'),
               '-Dscms.evidence-dir=' + str(out / 'workflow'), *props, 'test']
    with (out / 'postgres.log').open('w', encoding='utf-8') as log:
        result = subprocess.run(command, cwd=dest, stdout=log, stderr=subprocess.STDOUT)
    suites = []
    for _, name, _ in SUITES:
        files = list((dest / 'target/surefire-reports').glob('TEST-*.' + name + '.xml'))
        if not files:
            suites.append({'suite': name, 'result': 'NOT_RUN'}); continue
        xml = ET.parse(files[0]).getroot()
        counts = {k: int(xml.get(k, 0)) for k in ('tests', 'failures', 'errors', 'skipped')}
        suites.append({'suite': name, **counts,
                       'result': 'PASS' if counts['tests'] and not any(counts[k] for k in
                           ('failures', 'errors', 'skipped')) else 'FAIL',
                       'cases': [{'name': c.get('name'), 'result':
                                  'FAIL' if c.find('failure') is not None or c.find('error') is not None
                                  else 'SKIPPED' if c.find('skipped') is not None else 'PASS'}
                                 for c in xml.findall('testcase')]})
    report = {'result': 'PASS_LOCAL_CHECKED_SCOPE' if result.returncode == 0 and
              all(s['result'] == 'PASS' for s in suites) else 'FAIL',
              'environment': 'PostgreSQL loopback 127.0.0.1:55440, source copy without .env',
              'sourceFingerprints': source_fingerprints(root), 'suites': suites,
              'total': {k: sum(s.get(k, 0) for s in suites) for k in ('tests', 'failures', 'errors', 'skipped')},
              'limits': ['Regression Clocks/fixtures are not current demo preconditions',
                         'No cloud SQL, provider or FE/browser acceptance']}
    (out / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'result': report['result'], 'total': report['total']}))
    return 0 if report['result'] == 'PASS_LOCAL_CHECKED_SCOPE' else 1


if __name__ == '__main__': sys.exit(main())
