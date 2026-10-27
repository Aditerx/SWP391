"""Compare sanitized local HTTP exchanges to the current Render OpenAPI snapshot.
This checks positive status/type/property compatibility, not production business execution or full JSON Schema validity.
"""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/'artifacts/fe-handoff/2026-10-27'
spec=json.loads((OUT/'render-openapi.json').read_text(encoding='utf-8'))
exchanges=json.loads((OUT/'http-exchanges.json').read_text(encoding='utf-8'))
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

report={'result':'PASS' if not issues else 'FAIL','localExchangeCount':len(exchanges),'positiveExchangesChecked':positive,
        'requestResponseBodyChecks':body_checks,'uniqueIssues':sorted(set(tuple(x) for x in issues)),
        'observedNullablePaths':[{'operation':op,'fieldPath':field} for op,field in sorted(nullable)],
        'method':'Local successful HTTP exchanges checked against published Render status, basic JSON types and declared property names',
        'limits':['Does not execute authenticated production requests or business mutations',
                  'Not a complete JSON Schema validator: nullability, formats, ranges, patterns, required fields and negative error schemas need their own contract checks',
                  'Null values are preserved as observations and are not rejected merely because SpringDoc omitted nullable metadata; FE must handle these fields',
                  'Routes not exercised by these exchanges do not gain business-test coverage from this check']}
(OUT/'wire-schema-checks.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps({k:report[k] for k in ['result','localExchangeCount','positiveExchangesChecked','requestResponseBodyChecks','uniqueIssues']}))
raise SystemExit(0 if not issues else 1)
