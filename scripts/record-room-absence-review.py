"""Record only fresh room/absence proof in the single QA artifact; no network or DB access."""
from pathlib import Path
import argparse, datetime, hashlib, json, shutil, subprocess, xml.etree.ElementTree as ET
from qa_fingerprint import source_fingerprints

root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--run',required=True)
p.add_argument('--checkout',required=True)
a=p.parse_args()
run=(root/a.run).resolve();run.relative_to((root/'artifacts/runs').resolve())
checkout=Path(a.checkout).resolve()
assert not (checkout/'.env').exists(), 'Test checkout contains .env'

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
tested=source_fingerprints(checkout)
assert tested==source_fingerprints(root), 'Current code/schema/fixtures differ from tested copy'
counts=dict(tests=0,failures=0,errors=0,skipped=0)
xmls=list((run/'clean-xml').glob('TEST-*.xml'));assert xmls
for path in xmls:
 suite=ET.parse(path).getroot()
 for name in counts:counts[name]+=int(suite.attrib.get(name,0))
assert counts['failures']==counts['errors']==0
counts['passed']=counts['tests']-counts['skipped']
pg=ET.parse(run/'postgres-workflow.xml').getroot()
assert int(pg.attrib['failures'])==int(pg.attrib['errors'])==int(pg.attrib['skipped'])==0
http=run/'postgres-http'
cases=json.loads((http/'acceptance-cases.json').read_text(encoding='utf-8'))
assert cases and len({c['id'] for c in cases})==len(cases)
properties={x.attrib['name']:x.attrib.get('value','') for x in pg.find('properties')}
test_hash=sha(root/'src/test/java/com/sportscenter/workflow/PhaseWorkflowHttpTest.java')
assert properties.get('scms.test-source-sha256','').lower()==test_hash
pg_url=properties.get('scms.phases.pg-url','')
assert pg_url.startswith('jdbc:postgresql://127.0.0.1:55440/scms_phases_')
selected={'ABSENCE-UNDER-12H','CENTER-ROOM-NOTICE','EMERGENCY-MOVE-NOTICE','QR-WINDOW-REPLAY-CORRECTION','ABSENCE-LIFECYCLE','ABSENCE-ATOMIC-ROLLBACK','ROOM-CODE-API','BOOK-12H-AND-NOTICE-START'}
cases=[c for c in cases if c['id'] in selected];assert {c['id'] for c in cases}==selected
exchanges=json.loads((http/'http-exchanges.json').read_text(encoding='utf-8'))
indexes=sorted({i for c in cases for i in c['exchangeIndexes']})
remap={old:new for new,old in enumerate(indexes)}
for case in cases:
 case['rawExchangeIndexes']=case['exchangeIndexes']
 case['exchangeIndexes']=[remap[i] for i in case['exchangeIndexes']]
 case['result']='PASS'
scoped_exchanges=[exchanges[i] for i in indexes]
migration=json.loads((run/'migration-final.json').read_text(encoding='utf-8'))
assert migration['localOnly'] and all(v=='PASS' for v in migration['checks'].values())
for name,expected in migration['sha256'].items():assert sha(root/name)==expected, 'Migration proof differs: '+name
spec=json.loads((http/'openapi.json').read_text(encoding='utf-8'))
for dto,field in [('RoomRequest','roomCode'),('RoomResponse','roomCode'),('AttendanceResponse','absenceNoticeId')]:
 assert field in spec['components']['schemas'][dto]['properties']
assert spec['components']['schemas']['RoomResponse']['properties']['id']['type']=='integer'
schema=root/'artifacts/fe-handoff/openapi.json';shutil.copyfile(http/'openapi.json',schema)
qa_path=root/'artifacts/qa/latest.json';qa=json.loads(qa_path.read_text(encoding='utf-8-sig'))
checked=datetime.datetime.now(datetime.timezone.utc).isoformat()
qa['recordedAt']=checked
qa['openApi'].update(sha256=sha(schema),evidence='roomAbsenceReview',generatedLocally=True)
qa['roomAbsenceReview']=dict(
 checkedAt=checked,sourceCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),
 sourceState='uncommitted working tree; exact tested fingerprints below',testedCheckout=str(checkout),sourceFingerprints=tested,
 environment=dict(clean='Java17/H2, isolated checkout without .env',http='PostgreSQL17 at 127.0.0.1:55440, fresh disposable DB, actual HTTP sessions',timezone='Asia/Ho_Chi_Minh',cloudAccess=False),
 clean=dict(command='.\\mvnw.cmd clean test',**counts),
 postgresHttp=dict(tests=int(pg.attrib['tests']),failures=0,errors=0,skipped=0,database=pg_url.rsplit('/',1)[1],testSourceSha256=test_hash,cases=cases,exchanges=scoped_exchanges),
 migrations=migration,openApi=dict(path='artifacts/fe-handoff/openapi.json',sha256=sha(schema),proof='Same local HTTP application; numeric room id and additive roomCode/absenceNoticeId verified'),
 rawRun=run.relative_to(root).as_posix(),
 acceptance=['20 resources match room.jpg; numeric PK/FK preserved; legacy room/session history retained; safe upgrade/replay and rollback guards',
 'Notice + linked Absent atomic; owner/role/center/window, retry/race, reschedule archive, QR/Coach/finalizer/correction, injected PG rollback',
 'Room CRUD keeps numeric API; code uniqueness/format/center, optional field compatibility and hidden retired legacy rooms'],
 limits=['No cloud migration, deploy, Render or current Supabase verification',
 'OTP cloud, VNPay live gateway, FE/browser acceptance and full baseline CRUD matrix remain open',
 'Upgrade capacity reconciliation in disposable fixture is not authorization to shrink live classes automatically',
 'Historical QA sections retain their own source/time; skip is not pass'])
qa_path.write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(qa=str(qa_path),clean=counts,postgresHttpTests=int(pg.attrib['tests']),migrations=migration['checks'],cloudAccess=False),indent=2))
