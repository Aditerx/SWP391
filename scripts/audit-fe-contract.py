"""Offline verification of the one FE schema and retained QA record. No network, login, DB or document generation."""
from pathlib import Path
import argparse,datetime as dt,hashlib,json,re,subprocess
from qa_fingerprint import source_fingerprints
ROOT=Path(__file__).resolve().parents[1]
METHODS={'get','post','put','patch','delete','head','options'}
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--openapi',default='artifacts/fe-handoff/openapi.json')
p.add_argument('--qa',default='artifacts/qa/latest.json')
p.add_argument('--output',help='Optional single JSON result; default prints only')
a=p.parse_args()
openapi_path=(ROOT/a.openapi).resolve();qa_path=(ROOT/a.qa).resolve()
spec=json.loads(openapi_path.read_text(encoding='utf-8-sig'))
qa=json.loads(qa_path.read_text(encoding='utf-8-sig'))
fresh=qa.get('roomAbsenceReview')
exchanges=fresh['postgresHttp']['exchanges'] if fresh else qa['local']['exchanges']
def canonical(path): return re.sub(r'\{[^}]+\}','{}',path.rstrip('/'))

def annotation(text,start):
    """Read balanced Java annotation arguments, including nested annotations/string literals."""
    at=text.find('(',start);depth=0;quoted=False;escaped=False
    for i in range(at,len(text)):
        c=text[i]
        if quoted:
            if escaped:escaped=False
            elif c=='\\':escaped=True
            elif c=='"':quoted=False
        elif c=='"':quoted=True
        elif c=='(':depth+=1
        elif c==')':
            depth-=1
            if depth==0:return text[at+1:i],i+1
    raise ValueError('Unbalanced annotation')

def paths(args):
    vals=re.findall(r'"([^"\n]*)"',args)
    return [v for v in vals if v.startswith('/')] or ['']

handlers={}
for file in sorted((ROOT/'src/main/java').rglob('*Controller.java')):
    text=file.read_text(encoding='utf-8-sig');cls=re.search(r'\bclass\s+(\w+)',text)
    if not cls:continue
    prefix=text[:cls.start()];base_match=re.search(r'@RequestMapping\s*\(',prefix)
    bases=paths(annotation(prefix,base_match.start())[0]) if base_match else ['']
    class_auth=re.findall(r'@PreAuthorize\("([^"\n]*)"\)',prefix)
    for m in re.finditer(r'@(Get|Post|Put|Patch|Delete|Request)Mapping\b',text[cls.end():]):
        start=cls.end()+m.start(); end_name=cls.end()+m.end()
        if text[end_name:].lstrip().startswith('('):args,end=annotation(text,start)
        else:args,end='',end_name
        signature=re.search(r'\bpublic\s+[\w.<>,?\[\]\s]+?\s+(\w+)\s*\(',text[end:])
        if not signature:continue
        method_start=end+signature.start(); method_name=signature.group(1)
        _,params_end=annotation(text,end+signature.end()-1)
        # Controller method bodies used here have ordinary Java block formatting.
        # Read through the next mapping only; status evidence is explicit ResponseEntity calls.
        next_mapping=re.search(r'@(Get|Post|Put|Patch|Delete|Request)Mapping\b',text[params_end:])
        body=text[params_end:params_end+next_mapping.start()] if next_mapping else text[params_end:]
        codes=[]
        if re.search(r'ResponseEntity\.status\((?:HttpStatus\.)?CREATED\)|ResponseEntity\.status\(201\)',body):codes.append(201)
        if 'ResponseEntity.noContent()' in body:codes.append(204)
        if 'ResponseEntity.ok(' in body or not codes:codes.append(200)
        previous_end=max(text.rfind('}',cls.end(),start),cls.end())
        method_annotations=text[previous_end+1:method_start]
        auths=re.findall(r'@PreAuthorize\("([^"\n]*)"\)',method_annotations)
        declared=re.search(r'@ResponseStatus\(\s*(?:value\s*=\s*)?(?:HttpStatus\.)?(\w+)',method_annotations)
        if declared and codes==[200]:codes=[{'CREATED':201,'NO_CONTENT':204,'OK':200}.get(declared.group(1),200)]
        expression=auths[-1] if auths else class_auth[-1] if class_auth else None
        verbs=[m.group(1).upper()]
        if m.group(1)=='Request':verbs=re.findall(r'RequestMethod\.(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)',args)
        for base in bases:
            for suffix in paths(args):
                path=base.rstrip('/')+'/'+suffix.lstrip('/') if suffix else base
                for verb in verbs:
                    key=(verb,canonical(path))
                    if key in handlers:raise ValueError('Duplicate source route '+str(key))
                    handlers[key]={'file':file.relative_to(ROOT).as_posix(),'line':text.count('\n',0,start)+1,
                        'controller':cls.group(1),'method':method_name,'authorizationExpression':expression,'path':path,
                        'successStatusesExplicitInController':sorted(set(codes))}

issues=[];nullable=set();positive=0;body_checks=0
def check(value,schema,where,operation):
    if '$ref' in schema:return check(value,spec['components']['schemas'][schema['$ref'].split('/')[-1]],where,operation)
    if value is None:nullable.add((operation,where));return
    expected=schema.get('type')
    types={'object':isinstance(value,dict),'array':isinstance(value,list),'string':isinstance(value,str),
           'boolean':isinstance(value,bool),'integer':isinstance(value,int) and not isinstance(value,bool),
           'number':isinstance(value,(int,float)) and not isinstance(value,bool)}
    if expected and not types.get(expected,True):issues.append([operation,where,expected,type(value).__name__]);return
    if isinstance(value,dict):
        properties=schema.get('properties',{})
        for key,item in value.items():
            if key in properties:check(item,properties[key],where+'.'+key,operation)
            elif 'additionalProperties' not in schema:issues.append([operation,where+'.'+key,'undeclared property',''])
    if isinstance(value,list):
        for item in value:check(item,schema.get('items',{}),where+'[]',operation)

for e in exchanges:
    if not 200<=e['actualStatus']<300:continue
    path=e['path'].split('?')[0];method=e['method'].lower()
    matches=[(p,item[method]) for p,item in spec['paths'].items() if method in item
             and re.fullmatch(re.sub(r'\{[^}]+\}',r'[^/]+',p),path)]
    if not matches:issues.append([e['method']+' '+path,'operation','missing','']);continue
    template,operation=sorted(matches,key=lambda kv:kv[0].count('{'))[0];label=e['method']+' '+template;positive+=1
    response=operation.get('responses',{}).get(str(e['actualStatus']))
    if response is None:issues.append([label,'status',e['actualStatus'],'not published']);continue
    for side,value,node in [('request',e['requestBody'],operation.get('requestBody',{})),('response',e['responseBody'],response)]:
        contents=node.get('content',{})
        if contents:body_checks+=1;check(value,next(iter(contents.values())).get('schema',{}),side,label)


operations=[(m.upper(),canonical(path)) for path,item in spec['paths'].items() for m in item if m in METHODS]
missing=[m+' '+path for m,path in operations if (m,path) not in handlers]
extra=[m+' '+h['path'] for (m,path),h in handlers.items() if (m,path) not in operations]
reference=fresh['sourceCommit'] if fresh else qa['results']['sourceCommit']
backend_match=(source_fingerprints(ROOT)==fresh['sourceFingerprints']) if fresh else subprocess.run(['git','diff','--quiet',reference,'--','src/main','pom.xml','database'],cwd=ROOT).returncode==0
source_verification='tested working-tree fingerprints' if fresh else 'Git backend diff against evidence commit'
sql_fix=qa.get('roomCatalogCompatibilityFix')
if fresh and sql_fix and sql_fix.get('result')=='PASS':
    baseline={key:fresh['sourceFingerprints'][key] for key in ('src/main','src/test','pom.xml')}
    proof=sql_fix.get('migrationProof',{})
    if sql_fix.get('unchangedBackendFingerprints')==baseline and proof.get('checks') and all(value=='PASS' for value in proof['checks'].values()):
        current=source_fingerprints(ROOT)
        backend_match=current==sql_fix['sourceFingerprints'] and all(current[key]==value for key,value in baseline.items())
        source_verification='unchanged retained BE/test source + fresh local SQL compatibility proof'
query_review=qa.get('queryDrImplementation')
if query_review and query_review.get('result')=='PASS_LOCAL':
    proof=query_review.get('validation',{})
    if (proof.get('cleanTest',{}).get('failures')==0 and proof.get('cleanTest',{}).get('errors')==0
            and proof.get('postgresHttp',{}).get('failures')==0 and proof.get('postgresHttp',{}).get('errors')==0
            and proof.get('migration',{}).get('result')=='PASS'):
        backend_match=source_fingerprints(ROOT)==query_review.get('sourceFingerprints')
        source_verification='QueryDR snapshot tested locally; concurrent workspace edits require separate verification; prior wire exchanges remain historical evidence'
schema_hash=hashlib.sha256(openapi_path.read_bytes()).hexdigest()
attendance_review=qa.get('attendanceImplementation20261109',{})
if attendance_review.get('result')=='PASS_LOCAL':
    clean=attendance_review.get('cleanTest',{})
    pg=attendance_review.get('postgresAttendance',{})
    if (clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('failures')==0 and pg.get('errors')==0
            and pg.get('skipped')==0
            and attendance_review.get('migration',{}).get('status')=='PASS'
            and schema_hash==attendance_review.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==attendance_review.get('sourceFingerprints')
        source_verification='Attendance09/11 tested local tree; retained wire exchanges remain historical and are checked only for structural compatibility'
hash_match=schema_hash==qa['openApi']['sha256']
attendance_release=qa.get('attendanceRelease20261109',{})
if attendance_release.get('result')=='PASS_LOCAL':
    clean=attendance_release.get('cleanVerify',{})
    pg=attendance_release.get('postgresAttendance',{})
    if (clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and schema_hash==attendance_release.get('openApi',{}).get('sha256')
            and attendance_release.get('migrationFileSha256')==attendance_review.get('migrationFile',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==attendance_release.get('sourceFingerprints')
        source_verification='Attendance release integrated with main and tested locally; retained wire exchanges remain historical structural evidence'
phase1=qa.get('phase1Implementation20261109',{})
if phase1.get('result')=='PASS_LOCAL':
    clean=phase1.get('cleanTest',{})
    pg=phase1.get('postgres',{}).get('aggregate',{})
    fixture=phase1.get('postCleanFixtureFollowUp',{})
    if (clean.get('failures')==0 and clean.get('errors')==0
            and clean.get('finalBeAndSchemaMatch') is True
            and fixture.get('result')=='PASS' and fixture.get('failures')==0 and fixture.get('errors')==0
            and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and phase1.get('migrations',{}).get('status')=='PASS'
            and schema_hash==phase1.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==phase1.get('sourceFingerprints')
        source_verification='Phase1 R3/validation/audit/publication-price/room local verification; retained wire exchanges remain historical structural evidence; FE/Render not accepted'
rebuild=qa.get('demoDbAuditRebuild20261109',{})
if rebuild.get('result')=='PASS_LOCAL':
    clean=rebuild.get('cleanTest',{})
    pg=rebuild.get('postgresHttp',{})
    sql_proof=rebuild.get('sqlReset',{})
    if (rebuild.get('unchangedBackendFingerprints')=={key:phase1.get('sourceFingerprints',{}).get(key) for key in ('src/main','pom.xml')}
            and clean.get('failures')==0 and clean.get('errors')==0 and clean.get('tests',0)>0
            and pg.get('tests',0)>0 and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and sql_proof.get('status')=='PASS' and sql_proof.get('cloneReset')=='PASS'
            and rebuild.get('postResetDataQuality')=={'checks':38,'nonzeroOrNotRun':0}
            and schema_hash==rebuild.get('openApiSha256')):
        current=source_fingerprints(ROOT)
        backend_match=(current==rebuild.get('sourceFingerprints')
                       and all(current[key]==value for key,value in rebuild['unchangedBackendFingerprints'].items()))
        source_verification='Demo schema rebuild validated on local clone/fresh/replay/JPA/authenticated workflows and clean tests; BE unchanged from Phase1; FE/Render not accepted'
catalog=qa.get('roleCatalogReadAccess20261110',{})
if catalog.get('result')=='PASS_LOCAL':
    clean=catalog.get('cleanTest',{})
    targeted=catalog.get('targeted',{})
    if (clean.get('tests',0)>0 and clean.get('failures')==0 and clean.get('errors')==0
            and targeted.get('tests',0)>0 and targeted.get('failures')==0 and targeted.get('errors')==0 and targeted.get('skipped')==0
            and catalog.get('openApi',{}).get('generatedFromTestedApp') is True
            and schema_hash==catalog.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==catalog.get('sourceFingerprints')
        source_verification='Admin-only role/permission GET catalogs and role-detail bypass guarded; current login/authority/status tests and clean regression local; FE/Render not accepted'
normal=qa.get('normalLifecycleFoundation20261110',{})
if normal.get('result')=='PASS_LOCAL_FOUNDATION_PHASE1_INCOMPLETE':
    clean=normal.get('cleanTest',{})
    pg=normal.get('postgresNormal',{})
    if (normal.get('phase1Complete') is False
            and clean.get('tests',0)>0 and clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('tests',0)>0 and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and normal.get('migration',{}).get('status')=='PASS'
            and normal.get('schemaReference',{}).get('status')=='PASS'
            and normal.get('openApi',{}).get('generatedFromTestedApp') is True
            and schema_hash==normal.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==normal.get('sourceFingerprints')
        source_verification='Normal lifecycle/admission foundation tested locally; Phase1 remains incomplete; retained wire exchanges are historical structural compatibility evidence only; no FE/Render acceptance'
wire_reference=reference
complete=qa.get('normalPhase1Implementation20261110',{})
if complete.get('result')=='PASS_LOCAL' and complete.get('phase1Complete') is True:
    clean=complete.get('cleanTest',{})
    pg=complete.get('postgresHttp',{})
    if (clean.get('tests',0)>0 and clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('tests',0)>0 and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and complete.get('schema',{}).get('status')=='PASS'
            and complete.get('openApi',{}).get('generatedFromTestedApp') is True
            and schema_hash==complete.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==complete.get('sourceFingerprints')
        hash_match=True
        reference=complete.get('gitIntegration',{}).get('head',complete.get('baseCommit',reference))
        source_verification='Normal Phase1 behavior/schema/approved synthetic fixture verified locally; historical wire rows remain structural compatibility evidence; FE/cloud/provider not accepted'
# Current scoped acceptance can verify the changed tree without relabelling the
# retained legacy wire rows as fresh business tests. Missing legacy schema is an
# explicit fingerprint value, not a skipped comparison.
session3=qa.get('session33Readiness20261113',{})
if session3.get('status') == 'PASS_LOCAL_CHECKED_SCOPE':
    clean=session3.get('cleanTest',{})
    pg=session3.get('postgres',{})
    source=session3.get('source',{})
    if (clean.get('tests',0)>0 and clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('tests',0)>0 and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and source.get('testedCopiesMatch') is True
            and schema_hash==session3.get('openApi',{}).get('sha256')):
        backend_match=source_fingerprints(ROOT)==source.get('fingerprints')
        hash_match=True
        reference=source.get('head',reference)
        source_verification='Session3 scoped local acceptance verifies the working tree; retained wire rows remain historical structural examples, not fresh business or FE/cloud acceptance'
release=qa.get('session123Release20261113',{}).get('predeploy',{})
if release.get('status') == 'PASS_LOCAL_INTEGRATED':
    clean=release.get('clean',{})
    pg=release.get('postgres',{})
    if (clean.get('tests',0)>0 and clean.get('failures')==0 and clean.get('errors')==0
            and pg.get('tests',0)>0 and pg.get('failures')==0 and pg.get('errors')==0 and pg.get('skipped')==0
            and schema_hash==release.get('openApiSha256')):
        backend_match=source_fingerprints(ROOT)==release.get('sourceFingerprints')
        hash_match=True
        reference=release.get('baseMain',reference)
        source_verification='Integrated Session1-3 and current main tested locally; retained wire examples remain historical; deployment and FE acceptance are recorded separately'
result={'result':'PASS' if not issues and not missing and not extra and hash_match and backend_match else 'REVIEW_REQUIRED',
        'mode':'OFFLINE_RETAINED_EVIDENCE_CHECK','checkedAtUtc':dt.datetime.now(dt.timezone.utc).isoformat(),
        'backendTestsExecuted':False,'liveProbePerformed':False,'evidenceSourceCommit':reference,
        'retainedWireEvidenceSourceCommit':wire_reference,
        'sourceVerification':source_verification,
        'sourceTreeMatchesAcceptanceEvidence':backend_match,
        'retainedBackendTreeMatchesSource':backend_match,'openApiHashMatchesRecord':hash_match,
        'operations':len(operations),'sourceRoutesMatched':len(operations)-len(missing),'missingSourceRoutes':missing,'extraSourceRoutes':extra,
        'positiveRecordedExchanges':positive,'bodyTypePropertyChecks':body_checks,'wireIssues':sorted(set(tuple(x) for x in issues)),
        'limits':['Static route parser, not a complete Java or authorization analysis',
                  'Compatibility covers recorded positive status/basic types/properties; nullability/required/format/range/negative errors need separate tests',
                  'This does not rerun business tests or certify the current production state']}
if a.output:
    output=(ROOT/a.output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps(result,ensure_ascii=False))
raise SystemExit(0 if result['result']=='PASS' else 2)
