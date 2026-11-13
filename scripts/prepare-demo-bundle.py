"""Generate a staged bundle and timed scenario plan; optional new loopback DB verification only.

Never reads .env, connects to Supabase, deploys, or invokes a provider.
"""
from pathlib import Path
import argparse
import contextlib
import io
import json
import re
import runpy
import subprocess
import sys
from datetime import datetime

from demo_anchor import anchor_date, demo_time, scenario_plan, TIMEZONE, VN, FIXTURE_VERSION

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--anchor', required=True, type=anchor_date)
p.add_argument('--demo-at', help='Optional planned ISO timestamp with offset; omitted means time windows are not evaluated')
p.add_argument('--output-dir', required=True, help='New/empty staging directory under artifacts/runs/')
p.add_argument('--verify-local', action='store_true', help='Run fresh/replay/rollback checks on a NEW disposable local DB')
p.add_argument('--database', help='Required with --verify-local: NEW scms_schema_demo_phase1_* database')
args = p.parse_args()
try:
    at = demo_time(args.demo_at, args.anchor) if args.demo_at else None
except ValueError as e:
    p.error(str(e))
output = (ROOT/args.output_dir).resolve()
staging_root = (ROOT/'artifacts/runs').resolve()
if output == staging_root or not output.is_relative_to(staging_root):
    p.error('Staging must be a child of artifacts/runs; canonical SQL/fixture files are not overwritten')
if output.exists() and any(output.iterdir()):
    p.error('Use a new/empty staging directory; existing results are retained')
if args.verify_local and not re.fullmatch(r'scms_schema_demo_phase1_[a-z0-9_]+', args.database or ''):
    p.error('--verify-local requires a NEW scms_schema_demo_phase1_* database')
if args.database and not args.verify_local:
    p.error('--database requires --verify-local')


def run(script, *values):
    result = subprocess.run([sys.executable, str(ROOT/'scripts'/script), *map(str, values)],
                            cwd=ROOT, capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise RuntimeError(script+' failed: '+result.stderr[-1800:])
    return json.loads(result.stdout.splitlines()[-1])


# Compile before creating output: invalid design or time input leaves no partial bundle.
old = sys.argv
try:
    sys.argv = ['build-system-data.py', '--anchor', args.anchor.isoformat(), '--no-render']
    with contextlib.redirect_stdout(io.StringIO()):
        design = runpy.run_path(str(ROOT/'scripts/build-system-data.py'))
finally:
    sys.argv = old
plan = scenario_plan(design['tables'], args.anchor, at)
output.mkdir(parents=True, exist_ok=True)
relative = lambda path: str(path.relative_to(ROOT)).replace('\\', '/')
generated = run('build-demo-schema.py', '--anchor', args.anchor.isoformat(),
                '--output', relative(output/'schema_postgres.sql'),
                '--fixture-output', relative(output/'scms_demo_scenarios.sql'))
(output/'demo-plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
verification = {'anchorDate': args.anchor.isoformat(), 'fixtureVersion': FIXTURE_VERSION,
                'timeZone': TIMEZONE, 'generated': {'status':'PASS', **generated},
                'scenarioPlan': relative(output/'demo-plan.json'),
                'postgres': {'status':'NOT_RUN','reason':'--verify-local not requested'},
                'httpBusiness': {'status':'NOT_RUN','reason':'Run the local HTTP checks separately; SQL checks do not certify HTTP'},
                'cloudExecuted':False,'envLoaded':False,'providerInvoked':False}
exit_code = 0
if args.verify_local:
    if args.anchor != datetime.now(VN).date():
        verification['postgres'] = {'status':'NOT_RUN', 'reason':'Installation anchor differs from current VN date; keep SQL guard. Verify on the intended day.'}
        exit_code = 2
    else:
        try:
            result = run('test-demo-schema-postgres.py', '--database',args.database,
                         '--schema',relative(output/'schema_postgres.sql'),
                         '--output',relative(output/'postgres-proof.json'))
            verification['postgres'] = {'status':'PASS','database':args.database,
                                        'evidence':relative(output/'postgres-proof.json')}
        except Exception as e:
            verification['postgres'] = {'status':'FAIL','reason':str(e)}
            exit_code = 1
(output/'verification.json').write_text(json.dumps(verification,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps({'output':relative(output),'anchorDate':args.anchor.isoformat(),
                  'generated':verification['generated']['status'],'postgres':verification['postgres']['status'],
                  'httpBusiness':'NOT_RUN','cloudExecuted':False}))
raise SystemExit(exit_code)
