"""Inventory every published operation against Java routes and produce a read-only Render GET audit.
Never loads .env, logs in, sends mail, calls a callback, or executes a production mutation.
"""
from pathlib import Path
import argparse, concurrent.futures, datetime as dt, hashlib, json, re, subprocess, time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT=Path(__file__).resolve().parents[1]
BASE='https://scms-app-p1gy.onrender.com'
METHODS={'get','post','put','patch','delete','head','options'}
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',default='artifacts/fe-handoff/2026-10-27')
p.add_argument('--probe-get',action='store_true',help='Anonymous GET only; no callback endpoints')
p.add_argument('--exchanges',help='Sanitized local HTTP exchange JSON from the committed demo runner')
a=p.parse_args(); OUT=(ROOT/a.output).resolve(); OUT.mkdir(parents=True,exist_ok=True)
spec=json.loads((OUT/'render-openapi.json').read_text(encoding='utf-8'))

def save(name,value):
    (OUT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

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

def public(method,path):
    return path=='/api/health' or (method=='GET' and path.startswith('/api/public/')) or \
        (method=='GET' and path in ('/api/payments/vnpay/ipn','/api/payments/vnpay/return')) or \
        (method=='POST' and path in ('/api/auth/login','/api/auth/logout','/api/auth/forgot-password',
         '/api/auth/reset-password','/api/auth/register','/api/auth/verify-registration','/api/auth/resend-otp'))

def decision_ids(path):
    if path.startswith('/api/advanced-registrations') or 'admission-capacity' in path or 'work-schedule' in path:return ['D03']
    if path.startswith('/api/center-incidents'):return ['D06','D08','D12','D14','D15']
    if path.startswith('/api/payment-resolutions'):return ['D10','D16']
    if path.startswith('/api/holidays'):return ['D12','D14']
    if '/book-private' in path:return ['D03','D04','D12']
    if '/cancel-private' in path or '/absence' in path:return ['D05']
    if '/reschedule' in path:return ['D06','D07','D08']
    if '/substitute-coach' in path:return ['D06']
    if path.startswith('/api/attendance') or path.startswith('/api/training-results'):return ['D15','D17']
    if path.startswith('/api/reports'):return ['D13','D18','D19']
    if '/invoices' in path or '/vnpay/' in path:return ['D03','D10','D16']
    if 'subscription' in path or 'member-packages' in path:return ['D03','D08','D12','D13']
    if path=='/api/coaches':return ['D09']
    if '/subjects' in path:return ['D02']
    if path.startswith('/api/classes') or path.startswith('/api/enrollments') or path.startswith('/api/sessions'):return ['D03','D12','D14','D15']
    return []

contract=(ROOT/'docs/01-API-CONTRACT-FE-BE.md').read_text(encoding='utf-8')
mentions={canonical(x.rstrip('.')) for x in re.findall(r'/api/[\w/{}/.:-]+',contract)}
operations=[]
for path,item in spec['paths'].items():
    for method,op in item.items():
        if method not in METHODS:continue
        verb=method.upper(); handler=handlers.get((verb,canonical(path)))
        operations.append({'method':verb,'path':path,'operationId':op.get('operationId'),'tags':op.get('tags',[]),
            'publicAtSecurityFilter':public(verb,path),'sourceHandler':handler,
            'decisionIds':decision_ids(path),'contractPathExplicitlyMentioned':canonical(path) in mentions,
            'parameters':op.get('parameters',[]),'requestBody':op.get('requestBody'),
            'publishedResponses':op.get('responses',{}),'localExchangeIndexes':[],
            'renderBusinessVerified':False,'renderAnonymousGet':None})
operations.sort(key=lambda x:(x['path'],x['method']))
missing_source=[x['method']+' '+x['path'] for x in operations if not x['sourceHandler']]
extra_source=[verb+' '+data['path'] for (verb,path),data in handlers.items()
              if not any(x['method']==verb and canonical(x['path'])==path for x in operations)]

def match_operation(method,url):
    path=url.split('?')[0]
    matches=[x for x in operations if x['method']==method and re.fullmatch(re.sub(r'\{[^}]+\}',r'[^/]+',x['path']),path)]
    return sorted(matches,key=lambda x:x['path'].count('{'))[0] if matches else None

if a.exchanges:
    raw=json.loads(Path(a.exchanges).read_text(encoding='utf-8-sig'))
    # Local callback signatures/payment links are evidence, never publish an executable signature.
    text=json.dumps(raw,ensure_ascii=False)
    text=re.sub(r'vnp_SecureHash=[^&"\\]*','vnp_SecureHash=[redacted]',text)
    exchanges=json.loads(text);save('http-exchanges.json',exchanges)
    for index,e in enumerate(exchanges):
        op=match_operation(e['method'],e['path'])
        if op:op['localExchangeIndexes'].append(index)

def get_probe(op):
    route=re.sub(r'\{[^}]+\}','1',op['path']);start=time.monotonic()
    record={'method':'GET','pathTemplate':op['path'],'pathRequested':route,'authenticated':False}
    # No cookie jar, credentials, authorization, user data query, or environment file.
    for attempt in range(2):
        try:
            with urlopen(Request(BASE+route,headers={'User-Agent':'SCMS-ReadOnly-Contract-Audit/1.0'}),timeout=20) as r:
                status,body=r.status,r.read()
        except HTTPError as e:status,body=e.code,e.read()
        except (OSError,URLError,TimeoutError) as e:status,body=0,type(e).__name__.encode()
        if status not in (0,502,504):break
    record.update(status=status,attempts=attempt+1,durationSeconds=round(time.monotonic()-start,3),bodySha256=hashlib.sha256(body).hexdigest())
    record['expected']='200 public catalog/identity' if op['publicAtSecurityFilter'] else '401/403 only; authorization gate, not business correctness'
    record['pass']=status==200 if op['publicAtSecurityFilter'] else status in (401,403)
    if op['publicAtSecurityFilter']:
        try:
            parsed=json.loads(body)
            record['responseBody']=parsed
        except ValueError:pass
    # A mistakenly open protected endpoint must not cause production PII to be saved.
    return record

if a.probe_get:
    candidates=[x for x in operations if x['method']=='GET' and '/api/payments/vnpay/' not in x['path']]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results=list(pool.map(get_probe,candidates))
    save('render-get-checks.json',{'checkedAtUtc':dt.datetime.now(dt.timezone.utc).isoformat(),
        'baseUrl':BASE,'envRead':False,'authenticated':False,'method':'GET only, callbacks excluded',
        'businessMutationsExecuted':False,'productionBodyPolicy':'Public catalog/identity only; protected bodies hashed, never published',
        'checks':results,'passed':sum(x['pass'] for x in results),'total':len(results)})
elif (OUT/'render-get-checks.json').exists():results=json.loads((OUT/'render-get-checks.json').read_text(encoding='utf-8'))['checks']
else:results=[]
for probe in results:
    op=next(x for x in operations if x['method']=='GET' and x['path']==probe['pathTemplate'])
    op['renderAnonymousGet']={k:probe[k] for k in ('status','pass','expected')}

schemas=spec.get('components',{}).get('schemas',{})
def typename(s):
    if not s:return 'unspecified'
    if '$ref' in s:return s['$ref'].split('/')[-1]
    if s.get('type')=='array':return 'array<'+typename(s.get('items',{}))+'>'
    if 'additionalProperties' in s:return 'object/map'
    return s.get('type','object')+('('+s['format']+')' if s.get('format') else '')
def escape(s):return str(s).replace('|','\\|').replace('\n',' ')
def body_description(value):
    return ', '.join(k+': '+typename(v.get('schema',{})) for k,v in (value or {}).get('content',{}).items()) or 'no body schema'

lines=['# Danh mục toàn bộ API Render và nguồn đối chiếu','',
    'Sinh từ snapshot OpenAPI và controller đang checkout. Schema/route khớp không chứng nhận nghiệp vụ.',
    'GET anonymous 401/403 chỉ chứng minh cổng đăng nhập. Không có phiên đăng nhập hoặc mutation production trong đợt audit này.',
    'Phạm vi role/owner/center còn có kiểm tra trong service; biểu thức annotation bên dưới không thay toàn bộ policy.',
    'Schema request/response đầy đủ: [schemas.md](schemas.md); mẫu HTTP thật local: [http-exchanges.json](http-exchanges.json).','',
    '| # | Method | Path | Dxx | Local HTTP exchanges | Render anonymous GET |',
    '|---|---|---|---|---|---|']
for n,op in enumerate(operations,1):
    probe=op['renderAnonymousGet'];label=str(probe['status']) if probe else 'chưa gọi'
    lines.append(f"| {n} | {op['method']} | `{op['path']}` | {','.join(op['decisionIds']) or 'baseline'} | {len(op['localExchangeIndexes'])} | {label} |")
lines+=['','## Chi tiết từng thao tác','']
for n,op in enumerate(operations,1):
    h=op['sourceHandler']
    lines += [f"### {n}. {op['method']} {op['path']}",'',f"- operationId: `{op['operationId']}`; Dxx: {','.join(op['decisionIds']) or 'baseline ngoài quyết định Dxx trực tiếp'}.",
        '- Authentication: '+('Public theo SecurityConfig.' if op['publicAtSecurityFilter'] else 'Session JSESSIONID bắt buộc.'),
        '- Controller permission: `'+((h or {}).get('authorizationExpression') or 'không có annotation riêng; vẫn chịu SecurityConfig/service guards')+'`.',
        '- Success status đọc từ controller: '+str((h or {}).get('successStatusesExplicitInController',[]))+'; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).',
        f"- Handler: `{h['file']}:{h['line']}` → `{h['method']}`." if h else '- Handler: UNMATCHED, cần kiểm thủ công.',
        '- Request body: '+body_description(op['requestBody'])+('; required' if (op['requestBody'] or {}).get('required') else '; optional/no body')+'.',
        '- Responses do OpenAPI khai báo: '+', '.join(code+' '+body_description(v) for code,v in op['publishedResponses'].items())+'.',
        '- Local HTTP exchange indexes (0-based): '+(', '.join(map(str,op['localExchangeIndexes'])) or 'chưa có trong demo9case')+'.',
        '- Render: '+(f"anonymous GET {op['renderAnonymousGet']['status']}; {op['renderAnonymousGet']['expected']}" if op['renderAnonymousGet'] else 'chưa gọi thao tác này trong đợt audit; không suy từ Swagger')+'.','']
    if op['parameters']:
        lines+=['| Parameter | In | Required | Type | Default / enum |','|---|---|---|---|---|']
        for v in op['parameters']:
            schema=v.get('schema',{})
            lines.append('| '+ ' | '.join(map(escape,[v.get('name'),v.get('in'),v.get('required',False),typename(schema),schema.get('enum',schema.get('default',''))]))+' |')
        lines+=['']
(OUT/'api-inventory.md').write_text('\n'.join(lines)+'\n',encoding='utf-8',newline='\n')
lines=['# Schema request/response từ OpenAPI Live','',
       'Đây là schema công bố, không phải response đã gọi. Required/nullable lấy nguyên snapshot; nullable không khai báo không có nghĩa response thực không thể null. Đối chiếu thêm DTO/trace trong contract chính.','']
for name,schema in sorted(schemas.items()):
    lines += ['## '+name,'','| Field | Type | Required in OpenAPI | Constraints / enum |','|---|---|---|---|']
    for field,value in schema.get('properties',{}).items():
        rules={k:v for k,v in value.items() if k not in ('type','format','$ref','items','properties','description')}
        lines.append('| '+' | '.join(map(escape,[field,typename(value),field in schema.get('required',[]),json.dumps(rules,ensure_ascii=False) if rules else '']))+' |')
    if not schema.get('properties'):lines.append('| (schema) | '+escape(typename(schema))+' | | '+escape(json.dumps(schema,ensure_ascii=False))+' |')
    lines+=['']
(OUT/'schemas.md').write_text('\n'.join(lines)+'\n',encoding='utf-8',newline='\n')
summary={'checkedAtUtc':dt.datetime.now(dt.timezone.utc).isoformat(),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'renderIdentity':json.loads((OUT/'render-identity.json').read_text(encoding='utf-8')),
    'paths':len(spec['paths']),'operations':len(operations),'schemas':len(schemas),'sourceHandlersMatched':len(operations)-len(missing_source),
    'missingSource':missing_source,'extraSource':extra_source,
    'contractPathsExplicitlyMentioned':sum(x['contractPathExplicitlyMentioned'] for x in operations),
    'operationsWithLocalExchanges':sum(bool(x['localExchangeIndexes']) for x in operations),
    'renderAnonymousGetChecks':len(results),'renderAnonymousGetPassed':sum(x['pass'] for x in results),
    'renderAuthenticatedBusinessOperationsTested':0,'productionMutationsExecuted':False,
    'publishedSuccessStatusGaps':[{'method':x['method'],'path':x['path'],'controller':x['sourceHandler']['successStatusesExplicitInController'],
        'openApi':list(x['publishedResponses'])} for x in operations if x['sourceHandler'] and
        any(str(code) not in x['publishedResponses'] for code in x['sourceHandler']['successStatusesExplicitInController'])],
    'limits':['Controller parser is static; service ownership/center/locking checks need separate policy review/tests',
              'Anonymous GET guards and OpenAPI do not prove full role matrix, error mapping, concurrency, callbacks or business outcomes',
              'P1.5/D01/D09/FE integration remain excluded; no production session credentials were supplied or guessed'],
    'openApiSha256':hashlib.sha256((OUT/'render-openapi.json').read_bytes()).hexdigest()}
save('api-inventory.json',operations);save('audit-summary.json',summary)
print(json.dumps(summary,ensure_ascii=False))
if missing_source or extra_source:raise SystemExit(2)
