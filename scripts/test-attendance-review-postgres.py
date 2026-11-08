"""D15 migration17 guard/replay/preservation on a new disposable loopback PostgreSQL DB; never reads .env."""
from pathlib import Path
import argparse
import json
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
args = p.parse_args()
if not re.fullmatch(r'scms_phases_attendance_migration_[a-z0-9_]+', args.database):
    p.error('Only a new attendance migration fixture is allowed')
container = 'scms-phases-20261027'
inspection = json.loads(subprocess.check_output(['docker', 'inspect', container], encoding='utf-8'))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]


def sql(statement, database=None, error=None):
    result = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-At',
                             '-U', 'postgres', '-d', database or args.database], input=statement,
                            encoding='utf-8', capture_output=True)
    if error:
        assert result.returncode != 0 and error in result.stderr, result.stderr
    elif result.returncode:
        raise RuntimeError(result.stderr)
    return result.stdout.strip()


migration = (Path(__file__).resolve().parents[1] / 'database/migrations/17_attendance_review_postgres.sql').read_text(encoding='utf-8')
sql('CREATE DATABASE ' + args.database, 'postgres')
sql(migration, error='Attendance baseline is required')
sql("""CREATE TABLE sessions(session_id INT PRIMARY KEY,session_date DATE,status TEXT,attendance_finalized BOOLEAN);
CREATE TABLE attendances(attendance_id INT PRIMARY KEY,state TEXT,note TEXT);
INSERT INTO sessions VALUES(1,'2026-11-08','Completed',true),(2,'2026-11-09','Scheduled',false);
INSERT INTO attendances VALUES(1,'Absent','Original member note');""")
sql(migration, error='Migration14 attendance history baseline is required')
assert sql("SELECT count(*) FROM information_schema.columns WHERE table_name='sessions' AND column_name='attendance_review_pending'") == '0'
sql('ALTER TABLE attendances ADD COLUMN superseded_at TIMESTAMP')
snapshot = sql("SELECT md5(row_to_json(a)::text) FROM attendances a; SELECT md5((to_jsonb(s)-'attendance_review_pending')::text) FROM sessions s ORDER BY session_id")
sql(migration)
assert sql('SELECT count(*) FROM sessions WHERE attendance_review_pending') == '0', 'Upgrade queued legacy sessions'
sql('UPDATE sessions SET attendance_review_pending=true WHERE session_id=2')
sql(migration)
assert sql('SELECT attendance_review_pending FROM sessions WHERE session_id=2') == 't', 'Replay lost queued review'
assert snapshot == sql("SELECT md5(row_to_json(a)::text) FROM attendances a; SELECT md5((to_jsonb(s)-'attendance_review_pending')::text) FROM sessions s ORDER BY session_id")
sql("INSERT INTO sessions(session_id,session_date,status,attendance_finalized) VALUES(3,'2026-11-10','Scheduled',false)")
assert sql('SELECT attendance_review_pending FROM sessions WHERE session_id=3') == 'f'
bad = args.database + '_bad'
sql('CREATE DATABASE ' + bad, 'postgres')
sql('CREATE TABLE sessions(session_id INT,session_date DATE,attendance_review_pending TEXT); CREATE TABLE attendances(note TEXT,superseded_at TIMESTAMP)', bad)
sql(migration, bad, error='Incompatible sessions.attendance_review_pending')
assert sql("SELECT count(*) FROM pg_indexes WHERE indexname='idx_sessions_attendance_review_pending'", bad) == '0'
print(json.dumps({'status': 'PASS', 'database': args.database, 'localOnly': True,
                  'checks': ['baseline guards', 'legacy not queued', 'replay preserves pending', 'attendance/session preserved', 'new default false', 'incompatible column rollback']}))
