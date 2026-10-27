"""Generate a reviewable, atomic empty-schema PostgreSQL release. Does not connect to a database."""
from pathlib import Path
import argparse,hashlib,json,re
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',default='artifacts/demo/phase-2/2026-10-27/release')
args=p.parse_args();out=(root/args.output).resolve();out.mkdir(parents=True,exist_ok=True)
files=['database/bootstrap/scms_structure_postgres.sql']+['database/migrations/'+n for n in [
    '06_schedule_safety_postgres.sql','07_session_subscription_postgres.sql','08_advanced_entitlements_postgres.sql',
    '09_approved_catalog_postgres.sql','10_session_incidents_postgres.sql','11_holiday_history_postgres.sql','12_payment_resolutions_postgres.sql']]
chunks=['-- SCMS phases 1.0-1.4 and 2. P1.5 / T / FE excluded. Empty public schema only.','BEGIN;']
manifest=[]
for name in files:
    data=(root/name).read_bytes();body=data.decode('utf-8-sig')
    body=re.sub(r'(?m)^(?:BEGIN;|COMMIT;)\s*$','',body)
    sha=hashlib.sha256(data).hexdigest();manifest.append({'path':name,'sha256':sha})
    chunks.extend(['-- SOURCE: '+name+' SHA256 '+sha,body])
chunks.append('COMMIT;')
bundle=('\n'.join(chunks)+'\n').encode('utf-8');(out/'scms-phases-empty-schema.sql').write_bytes(bundle)
plan={'scope':'Empty-schema structure + roles/permissions + center + approved catalog; no demo accounts or transactions',
      'transaction':'One atomic transaction; empty-public-schema guard; executor rechecks counts before commit',
      'bootstrapAdmins':0,'demoAccounts':0,'paidInvoices':0,'activeSubscriptions':0,
      'sources':manifest,'bundleSha256':hashlib.sha256(bundle).hexdigest(),'productionExecuted':False}
(out/'release-plan.json').write_text(json.dumps(plan,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(json.dumps({'bundle':str(out/'scms-phases-empty-schema.sql'),'sha256':plan['bundleSha256'],'databaseAccess':False}))
