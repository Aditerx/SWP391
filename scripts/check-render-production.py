"""Read-only HTTPS checks for the SCMS Render service. Never loads .env or authenticates."""
from pathlib import Path
import argparse, datetime as dt, hashlib, json, time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-url', default='https://scms-app-p1gy.onrender.com')
p.add_argument('--output', required=True)
p.add_argument('--source-commit', default='unknown')
p.add_argument('--require-runtime-identity', action='store_true')
p.add_argument('--expected-db-fingerprint', help='Non-secret target digest produced by the JDBC preflight')
p.add_argument('--pushed-at-utc', required=True, help='ISO timestamp; refuse to check before five minutes after this push')
a = p.parse_args()
base = a.base_url.rstrip('/')
target = urlsplit(base)
if target.scheme != 'https' or target.hostname != 'scms-app-p1gy.onrender.com' or target.path or target.query:
    p.error('Only the authorized Render HTTPS origin is accepted')
pushed = dt.datetime.fromisoformat(a.pushed_at_utc.replace('Z', '+00:00'))
now = dt.datetime.now(dt.timezone.utc)
if (now-pushed).total_seconds() < 300:
    p.error('Wait at least five minutes after the push before checking production')
report = {'checkedAtUtc': now.isoformat(), 'pushedAtUtc': pushed.isoformat(),
          'elapsedSincePushSeconds': (now-pushed).total_seconds(), 'sourceCommitPushed': a.source_commit,
          'baseUrl': base, 'method': 'GET only; no login, email, payment or database writes',
          'envRead': False, 'requests': [], 'assertions': {}}

def get(path):
    for attempt in range(3):
        start = time.monotonic()
        try:
            with urlopen(Request(base+path, headers={'User-Agent':'SCMS-ReadOnly-Smoke/1.0'}), timeout=25) as r:
                status, body, content = r.status, r.read(), r.headers.get('Content-Type','')
        except HTTPError as e:
            status, body, content = e.code, e.read(), e.headers.get('Content-Type','')
        except (URLError, TimeoutError, OSError) as e:
            status, body, content = 0, type(e).__name__.encode(), ''
        report['requests'].append({'path':path,'status':status,'attempt':attempt+1,
                                   'durationSeconds':round(time.monotonic()-start,3),
                                   'contentType':content,'sha256':hashlib.sha256(body).hexdigest()})
        if status not in (0,502,503,504): break
        if attempt < 2: time.sleep(2)
    try: parsed = json.loads(body)
    except (ValueError,UnicodeDecodeError): parsed = None
    return status, body.decode('utf-8', errors='replace'), parsed

status, _, health = get('/actuator/health')
report['assertions']['healthUp'] = status == 200 and isinstance(health,dict) and health.get('status') == 'UP'
report['health'] = health
status, _, ping = get('/api/health')
report['assertions']['applicationUp'] = status == 200 and isinstance(ping,dict) and ping.get('status') == 'UP'
if isinstance(ping,dict):
    report['runtimeIdentity'] = {k:ping.get(k) for k in ('diagnosticsVersion','buildCommit','databaseTargetFingerprint')}
if a.require_runtime_identity:
    identity = report.get('runtimeIdentity',{})
    report['assertions']['runtimeIdentityAvailable'] = identity.get('diagnosticsVersion') == '1'
    report['assertions']['runtimeCommitMatchesPush'] = identity.get('buildCommit') == a.source_commit
if a.expected_db_fingerprint:
    report['assertions']['databaseTargetMatchesExpected'] = report.get('runtimeIdentity',{}).get('databaseTargetFingerprint') == a.expected_db_fingerprint
status, html, _ = get('/swagger-ui/index.html')
report['assertions']['swaggerHtml'] = status == 200 and 'swagger-ui' in html
for asset in ('swagger-ui.css','swagger-ui-bundle.js','swagger-initializer.js'):
    status, _, _ = get('/swagger-ui/'+asset)
    report['assertions']['asset:'+asset] = status == 200
status, _, config = get('/v3/api-docs/swagger-config')
report['assertions']['swaggerConfig'] = status == 200 and isinstance(config,dict)
status, _, spec = get('/v3/api-docs')
report['assertions']['openApi'] = status == 200 and isinstance(spec,dict) and 'paths' in spec
if report['assertions']['openApi']:
    paths = spec['paths']; schemas = spec.get('components',{}).get('schemas',{})
    report['openApiVersion'], report['pathCount'] = spec.get('openapi'),len(paths)
    for path in ('/api/advanced-registrations','/api/center-incidents','/api/payment-resolutions','/api/holidays/preview'):
        report['assertions']['path:'+path] = path in paths
    for schema, fields in {'PrivateBookingRequest':['agreementConfirmed','requestKey'],
                           'RevenueReportResponse':['moneyReceived','moneyRefunded','netReceived'],
                           'MemberPackageResponse':['activatedAt','expiresAt']}.items():
        report['assertions']['schema:'+schema] = set(fields) <= set(schemas.get(schema,{}).get('properties',{}))
status, _, subjects = get('/api/public/subjects')
approved = {'quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban'}
report['assertions']['nineApprovedSubjects'] = status == 200 and isinstance(subjects,list) and len(subjects)==9 and {x.get('slug') for x in subjects} == approved
if isinstance(subjects,list): report['subjectSlugs'] = [x.get('slug') for x in subjects]
for path in ('/api/reports/dashboard','/api/members/me/invoices','/api/center-incidents'):
    status, _, _ = get(path)
    report['assertions']['anonymousDenied:'+path] = status in (401,403)
status, _, info = get('/actuator/info')
report['deploymentShaExposed'] = info.get('git',{}).get('commit',{}).get('id') if status == 200 and isinstance(info,dict) else None
report['result'] = 'PASS' if all(report['assertions'].values()) else 'FAIL'
out = Path(a.output); out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps({'result':report['result'],'checks':len(report['assertions']),
                  'failed':[k for k,v in report['assertions'].items() if not v], 'output':str(out)}))
raise SystemExit(0 if report['result']=='PASS' else 1)
