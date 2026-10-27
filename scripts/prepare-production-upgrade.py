"""Back up the selected production DB read-only, encrypt with Windows DPAPI, and prove upgrade on local clones.
Never executes migration SQL against the remote database. Never prints credentials or business rows.
"""
from pathlib import Path
from urllib.parse import urlsplit, parse_qs
import argparse, ctypes, ctypes.wintypes as wt, datetime as dt, hashlib, json, os, re, subprocess

ROOT = Path(__file__).resolve().parents[1]
CONTAINER = 'scms-phases-20261027'
STAMP = dt.datetime.now(dt.timezone(dt.timedelta(hours=7))).strftime('%Y%m%d_%H%M%S')
PRIVATE = ROOT/'artifacts/private-backups'
OUT = ROOT/'artifacts/production-upgrade/2026-10-27'
PRIVATE.mkdir(parents=True, exist_ok=True); OUT.mkdir(parents=True, exist_ok=True)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--target-fingerprint')
parser.add_argument('--restore-backup', help='Restore only into a new guarded local database; no remote access')
args = parser.parse_args()

class Blob(ctypes.Structure):
    _fields_ = [('size',wt.DWORD),('data',ctypes.POINTER(ctypes.c_ubyte))]

def dpapi(data, protect):
    if os.name != 'nt': raise RuntimeError('Windows DPAPI required for the private backup')
    source = (ctypes.c_ubyte*len(data)).from_buffer_copy(data)
    incoming, outgoing = Blob(len(data),source), Blob()
    crypt = ctypes.windll.crypt32
    if protect:
        call = crypt.CryptProtectData
        call.argtypes = [ctypes.POINTER(Blob),wt.LPCWSTR,ctypes.POINTER(Blob),wt.LPVOID,wt.LPVOID,wt.DWORD,ctypes.POINTER(Blob)]
        ok = call(ctypes.byref(incoming),'SCMS production public-schema backup',None,None,None,1,ctypes.byref(outgoing))
    else:
        call = crypt.CryptUnprotectData
        call.argtypes = [ctypes.POINTER(Blob),ctypes.POINTER(wt.LPWSTR),ctypes.POINTER(Blob),wt.LPVOID,wt.LPVOID,wt.DWORD,ctypes.POINTER(Blob)]
        ok = call(ctypes.byref(incoming),None,None,None,None,1,ctypes.byref(outgoing))
    if not ok: raise ctypes.WinError()
    try: return ctypes.string_at(outgoing.data,outgoing.size)
    finally:
        free=ctypes.windll.kernel32.LocalFree; free.argtypes=[wt.HLOCAL]; free.restype=wt.HLOCAL
        free(ctypes.cast(outgoing.data,wt.HLOCAL))

def command(command_args, data=None, env=None, label='command', expected=True):
    r = subprocess.run(command_args,input=data,capture_output=True,cwd=ROOT,env=env,timeout=180)
    if bool(r.returncode == 0) != expected:
        log=PRIVATE/(STAMP+'-'+label+'.log');log.write_bytes(r.stdout+b'\n'+r.stderr)
        # Only an explicitly known guard message is safe to publish; details/COPY rows stay private.
        match=re.search(rb'D02 duplicate subject has new references in [A-Za-z0-9_]+: reconcile first',r.stderr)
        raise RuntimeError(match.group().decode() if match else label+' failed; private diagnostic retained')
    return r.stdout

def local_sql(database, text, expected=True, label='local-sql'):
    if not re.fullmatch(r'scms_phases_upgrade_[a-z0-9_]+|postgres',database): raise RuntimeError('Disposable local database required')
    return command(['docker','exec','-i',CONTAINER,'psql','-X','-q','-A','-t','-v','ON_ERROR_STOP=1','-U','postgres','-d',database],
                   text.encode(),label=label,expected=expected).decode('utf-8').strip()

inspection=json.loads(command(['docker','inspect',CONTAINER],label='container-inspection'))[0]
if inspection['HostConfig']['PortBindings']['5432/tcp'] != [{'HostIp':'127.0.0.1','HostPort':'55440'}]:
    raise RuntimeError('Wrong local PostgreSQL target')

def restore(archive, suffix):
    database='scms_phases_upgrade_'+STAMP+suffix
    local_sql('postgres','CREATE DATABASE '+database+';',label='create-local-db')
    archive_path='/tmp/'+database+'.dump';list_path='/tmp/'+database+'.list'
    # A new PostgreSQL database already has public. Exclude only its CREATE SCHEMA TOC item.
    # All business objects/data remain included and every other restore error is fatal.
    try:
        command(['docker','exec','-i',CONTAINER,'sh','-c','umask 077; cat > "$1"','sh',archive_path],archive,label='stage-local-archive')
        toc=command(['docker','exec',CONTAINER,'pg_restore','--list',archive_path],label='archive-toc')
        toc=b'\n'.join(line for line in toc.splitlines() if not re.search(rb';\s+\d+\s+\d+\s+SCHEMA\s+-\s+public\s',line))+b'\n'
        command(['docker','exec','-i',CONTAINER,'sh','-c','umask 077; cat > "$1"','sh',list_path],toc,label='stage-local-toc')
        command(['docker','exec',CONTAINER,'pg_restore','--exit-on-error','--no-owner','--no-acl','--use-list',list_path,
                 '-U','postgres','-d',database,archive_path],label='restore-local')
    finally:
        subprocess.run(['docker','exec',CONTAINER,'rm','-f','--',archive_path,list_path],capture_output=True)
    return database

if args.restore_backup:
    path=Path(args.restore_backup).resolve()
    if not path.is_relative_to(PRIVATE.resolve()): parser.error('Use the private backup directory')
    database=restore(dpapi(path.read_bytes(),False),'_restore')
    print(json.dumps({'localRestore':database,'remoteAccess':False}));raise SystemExit(0)
if not args.target_fingerprint: parser.error('--target-fingerprint is required')

env={}
for line in (ROOT/'.env').read_text(encoding='utf-8-sig').splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key,value=line.split('=',1);env[key.strip()]=value.strip()
url=env.get('DB_URL','')
if not url.startswith('jdbc:postgresql://'): raise RuntimeError('Configured PostgreSQL JDBC required')
target=urlsplit(url[5:])
if target.username is not None or not target.hostname: raise RuntimeError('Separate DB_USERNAME/DB_PASSWORD required')
mode=parse_qs(target.query).get('sslmode',['require'])[-1]
if mode not in ('require','verify-ca','verify-full'): raise RuntimeError('TLS required')
fingerprint=hashlib.sha256(f'{target.hostname}:{target.port if target.port else -1}/{target.path}|{env.get("DB_USERNAME")}'.encode()).hexdigest()
if fingerprint != args.target_fingerprint: raise RuntimeError('Configured target changed')
backup=PRIVATE/('production-'+fingerprint[:12]+'-'+STAMP+'.dump.dpapi')
command(['git','check-ignore','--quiet',str(backup.relative_to(ROOT))],label='backup-gitignore')
remote_env=os.environ.copy()
remote_env.update(PGHOST=target.hostname,PGPORT=str(target.port or 5432),PGDATABASE=target.path.lstrip('/'),
                  PGUSER=env['DB_USERNAME'],PGPASSWORD=env['DB_PASSWORD'],PGSSLMODE=mode,PGCONNECT_TIMEOUT='10',
                  PGAPPNAME='scms-readonly-upgrade-backup',PGOPTIONS='-c default_transaction_read_only=on')
names=['PGHOST','PGPORT','PGDATABASE','PGUSER','PGPASSWORD','PGSSLMODE','PGCONNECT_TIMEOUT','PGAPPNAME','PGOPTIONS']
dump_command=['docker','exec']
for name in names: dump_command+=['--env',name]
dump_command += [CONTAINER,'pg_dump','--schema=public','--format=custom','--no-owner','--no-acl','--serializable-deferrable']
print('Creating read-only production backup; raw data stays private',flush=True)
archive=command(dump_command,env=remote_env,label='read-only-pg-dump')
backup.write_bytes(dpapi(archive,True));restored_archive=dpapi(backup.read_bytes(),False)
if hashlib.sha256(archive).digest()!=hashlib.sha256(restored_archive).digest(): raise RuntimeError('Encrypted backup roundtrip failed')
(OUT/'backup-record.json').write_text(json.dumps({'targetFingerprint':fingerprint,'remoteWrites':False,'backupEncrypted':True,
    'backupFile':str(backup.relative_to(ROOT)).replace('\\','/'),'cipherSha256':hashlib.sha256(backup.read_bytes()).hexdigest(),
    'dpapiRoundtripVerified':True,'publicSchemaOnly':True},indent=2)+'\n',encoding='utf-8',newline='\n')
database=restore(restored_archive,'')

def ident(name): return '"'+name.replace('"','""')+'"'
def rows_digest(table,columns,key):
    return '(SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,\',\' ORDER BY '+ident(key)+'),\'\')) FROM (SELECT '+','.join(map(ident,columns))+' FROM public.'+ident(table)+') t)'

tables={'users':'user_id','invoices':'invoice_id','member_packages':'subscription_id','class_enrollments':'enrollment_id',
        'classes':'class_id','sessions':'session_id','membership_packages':'package_id'}
snapshot={}
for table,key in tables.items():
    columns=json.loads(local_sql(database,"SELECT json_agg(column_name ORDER BY ordinal_position) FROM information_schema.columns WHERE table_schema='public' AND table_name='"+table+"';"))
    expression=rows_digest(table,columns,key)
    digest=local_sql(database,"SET TIME ZONE 'UTC'; SELECT "+expression+';')
    snapshot[table]={'columns':columns,'key':key,'expression':expression,'digest':digest,
                     'rows':int(local_sql(database,'SELECT count(*) FROM public.'+ident(table)+';'))}
catalog_columns=json.loads(local_sql(database,"SELECT json_agg(column_name ORDER BY ordinal_position) FROM information_schema.columns WHERE table_schema='public' AND table_name='subjects';"))
catalog_expression=rows_digest('subjects',catalog_columns,'subject_id')
catalog_digest=local_sql(database,"SET TIME ZONE 'UTC'; SELECT "+catalog_expression+';')
catalog=json.loads(local_sql(database,"SELECT json_agg(to_jsonb(s)-'description' ORDER BY subject_id) FROM subjects s;"))
files=['05_migration_notifications.sql','06_migration_chat.sql','06_schedule_safety_postgres.sql','07_session_subscription_postgres.sql',
       '08_advanced_entitlements_postgres.sql','09_approved_catalog_postgres.sql','10_session_incidents_postgres.sql',
       '11_holiday_history_postgres.sql','12_payment_resolutions_postgres.sql']
guards=["IF "+catalog_expression+" <> '"+catalog_digest+"' THEN RAISE EXCEPTION 'Catalog changed; refresh reviewed backup'; END IF;"]
checks=["IF "+s['expression']+" <> '"+s['digest']+"' THEN RAISE EXCEPTION 'Original rows changed: "+t+"'; END IF;" for t,s in snapshot.items()]
chunks=['-- Existing-schema upgrade only; no bootstrap/reset or entitlement backfill.','BEGIN;',"SET LOCAL TIME ZONE 'UTC';",
        "SET LOCAL lock_timeout='5s';","SET LOCAL statement_timeout='30s';",
        'LOCK TABLE public.subjects, public.users, public.invoices, public.member_packages, public.class_enrollments, public.classes, public.sessions, public.membership_packages IN SHARE ROW EXCLUSIVE MODE;',
        'DO $guard$ BEGIN\n'+'\n'.join(guards+checks)+'\nEND $guard$;']
sources=[]
for name in files:
    path=ROOT/'database/migrations'/name;body=path.read_text(encoding='utf-8-sig')
    if re.search(r'\b(?:DROP\s+TABLE|TRUNCATE)\b','\n'.join(x for x in body.splitlines() if not x.lstrip().startswith('--')),re.I): raise RuntimeError('Destructive table operation refused')
    sha=hashlib.sha256(body.encode()).hexdigest();sources.append({'path':str(path.relative_to(ROOT)).replace('\\','/'),'normalizedLfSha256':sha})
    body=re.sub(r'(?m)^(?:BEGIN;|COMMIT;)\s*$','',body)
    chunks += ['-- SOURCE '+name+' SHA256 '+sha,body]
chunks += ['DO $verify$ BEGIN\n'+'\n'.join(checks)+"\nIF (SELECT count(*) FROM subjects) <> 9 THEN RAISE EXCEPTION 'Nine approved subjects required'; END IF;\nEND $verify$;",'COMMIT;']
bundle=('\n'.join(chunks)+'\n').encode();bundle_path=OUT/'scms-existing-production-upgrade.sql';bundle_path.write_bytes(bundle)
plan={'targetFingerprint':fingerprint,'remoteWrites':False,'backupEncrypted':True,'backupFile':str(backup.relative_to(ROOT)).replace('\\','/'),
      'encryptedBackupSha256':hashlib.sha256(backup.read_bytes()).hexdigest(),'backupRestoreVerified':True,
      'localProofDatabase':database,'bundleSha256':hashlib.sha256(bundle).hexdigest(),'sources':sources,
      'beforeCounts':{t:s['rows'] for t,s in snapshot.items()},'beforeCatalog':catalog,
      'originalRowDigests':{t:s['digest'] for t,s in snapshot.items()},'status':'LOCAL_PROOF_RUNNING'}
try:
    print('Testing the exact upgrade on a restored local clone',flush=True)
    local_sql(database,bundle.decode(),label='upgrade-local')
    for table,s in snapshot.items():
        if local_sql(database,"SET TIME ZONE 'UTC'; SELECT "+s['expression']+';')!=s['digest']: raise RuntimeError('Original local rows changed')
    for name in files: local_sql(database,(ROOT/'database/migrations'/name).read_text(encoding='utf-8-sig'),label='migration-replay')
    for table,s in snapshot.items():
        if local_sql(database,"SET TIME ZONE 'UTC'; SELECT "+s['expression']+';')!=s['digest']: raise RuntimeError('Migration replay changed original rows')
    local_sql(database,bundle.decode(),expected=False,label='bundle-replay-guard')
    guard_db=restore(restored_archive,'_guard')
    local_sql(guard_db,'CREATE TABLE scms_upgrade_guard_reference(id INT PRIMARY KEY,subject_id INT REFERENCES subjects(subject_id)); INSERT INTO scms_upgrade_guard_reference VALUES(1,1);')
    local_sql(guard_db,bundle.decode(),expected=False,label='referenced-catalog-guard')
    if int(local_sql(guard_db,'SELECT count(*) FROM subjects;'))!=len(catalog): raise RuntimeError('Guard failure did not preserve catalog')
    missing=local_sql(guard_db,"SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='member_packages' AND column_name='expires_at';")
    if missing!='0': raise RuntimeError('Failed guarded bundle did not roll back schema')
    plan.update(status='LOCAL_PROOF_PASS',originalRowsPreserved=True,sourceMigrationsReplay=True,bundleReplayRefused=True,
                referencedCatalogRefusedAndRolledBack=True,guardDatabase=guard_db,afterCatalogCount=9)
except Exception as e:
    plan.update(status='LOCAL_PROOF_FAILED',blocker=str(e))
    raise
finally:
    (OUT/'upgrade-plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps({'status':plan['status'],'bundleSha256':plan['bundleSha256'],'targetFingerprint':fingerprint,
                  'beforeCounts':plan['beforeCounts'],'afterCatalogCount':9,'remoteWrites':False}))
