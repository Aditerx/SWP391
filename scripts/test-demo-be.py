"""Run current BE sources in one reusable local snapshot without .env or cloud credentials.

The snapshot is a build directory, never a Git checkout. Each run keeps its own
XML/log/source fingerprint; concurrent use of its Maven target is rejected.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

from qa_fingerprint import source_fingerprints

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--run', required=True, help='New evidence directory name')
p.add_argument('--test', help='Surefire class/method selection; omitted means clean test')
p.add_argument('--property', action='append', default=[], help='Additional scms.* test property')
args = p.parse_args()
if not re.fullmatch(r'[a-z0-9_-]+', args.run):
    p.error('Run name must contain only lowercase letters, digits, underscore or dash')
if args.test and not re.fullmatch(r'[A-Za-z0-9_,#*]+', args.test):
    p.error('Invalid test selection')
for value in args.property:
    key, sep, data = value.partition('=')
    if not sep or not re.fullmatch(r'scms\.[a-z0-9.-]+', key):
        p.error('Only explicit scms.* test properties are supported')
    if 'pg-url' in key and not re.fullmatch(r'jdbc:postgresql://127\.0\.0\.1:55440/scms_(phases|schema|querydr)_[a-z0-9_]+', data):
        p.error('Only the disposable loopback PostgreSQL target is supported')

base = ROOT / 'artifacts/runs/be-demo-20261119'
out = base / args.run
if out.exists():
    p.error('Use a new run name; previous evidence is retained')
source = base / 'source'
source.mkdir(parents=True, exist_ok=True)
lock = source / '.validation-lock'
try:
    handle = lock.open('x', encoding='utf-8')
except FileExistsError:
    p.error('The isolated Maven target is already in use; do not run a second build')
handle.write(str(os.getpid()))
handle.close()
try:
    for directory in ('src', '.mvn', 'database', 'scripts'):
        shutil.copytree(ROOT / directory, source / directory, dirs_exist_ok=True,
                        ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
    for name in ('pom.xml', 'mvnw', 'mvnw.cmd'):
        shutil.copy2(ROOT / name, source / name)
    if (source / '.env').exists() or list(source.glob('.env.*')):
        raise RuntimeError('Credential/config file found in the isolated build directory')
    before = source_fingerprints(ROOT)
    if before != source_fingerprints(source):
        raise RuntimeError('The source snapshot does not match the working tree')
    out.mkdir(parents=True)
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    evidence = {'status': 'RUNNING', 'baseSha': head, 'uncommittedSources': True,
                'environment': 'Isolated source snapshot; Java17; no .env; test-owned H2 or explicit loopback PostgreSQL',
                'sourceFingerprints': before, 'selection': args.test or 'clean test',
                'cloudSqlProviderFrontend': 'NOT_RUN'}
    report = out / 'validation.json'
    report.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
    command = [str(source / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')),
               '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8',
               '-Dserver.address=127.0.0.1',
               '-Dlogging.level.org.springframework.web=OFF',
               '-Dlogging.level.org.hibernate.SQL=OFF',
               '-Dspring.config.import=optional:classpath:/scms-isolated-test.properties']
    command += ['-D' + value for value in args.property]
    command += ['-Dtest=' + args.test, 'test'] if args.test else ['clean', 'test']
    # Explicit test keys/configuration live in the test fixtures. Never inherit cloud configuration.
    prefixes = ('DB_', 'MAIL_', 'SMTP_', 'VNPAY_', 'SCMS_', 'PAYMENT_', 'DEEPSEEK_', 'GOOGLE_')
    env = {key: value for key, value in os.environ.items()
           if not key.upper().startswith(prefixes)
           and key.upper() not in ('SPRING_APPLICATION_JSON', 'SPRING_CONFIG_IMPORT', 'SPRING_CONFIG_LOCATION', 'SPRING_CONFIG_ADDITIONAL_LOCATION')}
    started = time.time()
    with (out / 'maven.log').open('w', encoding='utf-8') as log:
        run = subprocess.run(command, cwd=source, env=env, stdout=log, stderr=subprocess.STDOUT)
    xml_dir = out / 'reports'
    xml_dir.mkdir()
    counts = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
    suites = []
    for path in sorted((source / 'target/surefire-reports').glob('TEST-*.xml')):
        if path.stat().st_mtime < started - 1:
            continue
        xml = ET.parse(path).getroot()
        item = {'suite': xml.get('name'), **{key: int(xml.get(key, 0)) for key in counts}}
        suites.append(item)
        for key in counts:
            counts[key] += item[key]
        shutil.copy2(path, xml_dir / path.name)
    unchanged = before == source_fingerprints(ROOT) == source_fingerprints(source)
    # The main config may mention the optional import, but the file must never have been loaded.
    log_text = (out / 'maven.log').read_text(encoding='utf-8', errors='replace')
    loaded_env = "Config resource 'file [.env]'" in log_text
    passed = run.returncode == 0 and suites and not counts['failures'] and not counts['errors'] and unchanged and not loaded_env
    evidence.update(status='PASS_CHECKED_SCOPE' if passed else 'FAIL', exitCode=run.returncode,
                    counts=counts, executed=counts['tests']-counts['skipped'], suites=suites,
                    sourceUnchangedDuringRun=unchanged, dotenvLoaded=loaded_env,
                    logSha256=hashlib.sha256((out / 'maven.log').read_bytes()).hexdigest())
    report.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: evidence[key] for key in ('status', 'exitCode', 'counts', 'sourceUnchangedDuringRun', 'dotenvLoaded')}))
    raise SystemExit(0 if passed else 1)
finally:
    lock.unlink(missing_ok=True)
