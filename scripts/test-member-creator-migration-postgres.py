"""Check member creator migration on a NEW disposable loopback PostgreSQL DB; never loads .env."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CONTAINER = 'scms-phases-20261027'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--database', required=True)
parser.add_argument('--output', required=True)
args = parser.parse_args()
if not re.fullmatch(r'scms_member_creator_[a-z0-9_]+', args.database):
    parser.error('A NEW scms_member_creator_* local database is required')
output = (ROOT / args.output).resolve()
if not output.is_relative_to(ROOT):
    parser.error('Output must stay in the workspace')
inspection = json.loads(subprocess.check_output(['docker', 'inspect', CONTAINER]))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]
migration_path = ROOT / 'database/migrations/23_member_registered_by_postgres.sql'
migration = migration_path.read_text(encoding='utf-8')
checks = []


def sql(statement, database=None, error=None):
    result = subprocess.run(
        ['docker', 'exec', '-i', CONTAINER, 'psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1',
         '-U', 'postgres', '-d', database or args.database],
        input=statement, text=True, encoding='utf-8', capture_output=True, timeout=80)
    if error:
        assert result.returncode != 0 and error in result.stderr, result.stderr[-1600:]
    else:
        assert result.returncode == 0, result.stderr[-1600:]
    return result.stdout.strip()


def check(name):
    checks.append({'name': name, 'status': 'PASS'})


def snapshot():
    return sql("""SELECT json_build_object(
      'users',(SELECT json_agg(u ORDER BY user_id) FROM users u),
      'members',(SELECT json_agg(m ORDER BY user_id) FROM members m),
      'receptionists',(SELECT json_agg(r ORDER BY user_id) FROM receptionists r),
      'unrelated',(SELECT json_agg(s ORDER BY id) FROM migration_sentinel s),
      'sequence',(SELECT json_build_object('lastValue',last_value,'isCalled',is_called) FROM users_user_id_seq),
      'constraints',(SELECT json_agg(x ORDER BY conname) FROM
        (SELECT conname,pg_get_constraintdef(oid) AS definition,convalidated FROM pg_constraint
         WHERE conrelid='members'::regclass) x));""")


sql('CREATE DATABASE ' + args.database, 'postgres')  # Existing DB fails; never reset another DB.
sql("""CREATE TABLE users(user_id SERIAL PRIMARY KEY, role_name TEXT NOT NULL);
CREATE TABLE receptionists(user_id INT PRIMARY KEY REFERENCES users(user_id));
CREATE TABLE members(user_id INT PRIMARY KEY REFERENCES users(user_id), registered_by INT NULL,
 CONSTRAINT fk_member_receptionist FOREIGN KEY(registered_by) REFERENCES receptionists(user_id));
CREATE TABLE migration_sentinel(id INT PRIMARY KEY, amount NUMERIC(18,2), attendance TEXT);
INSERT INTO users VALUES(101,'CenterManager'),(102,'Receptionist'),(103,'Member'),(104,'Member');
INSERT INTO receptionists VALUES(102);
INSERT INTO members VALUES(103,102),(104,NULL);
INSERT INTO migration_sentinel VALUES(391,1250000.00,'Present');
SELECT setval('users_user_id_seq',700,true);""")
before = snapshot()
sql("BEGIN; INSERT INTO users VALUES(105,'Member'); INSERT INTO members VALUES(105,101); COMMIT;",
    error='fk_member_receptionist')
assert snapshot() == before
check('old_fk_rejects_manager_creator_and_rolls_back_new_user')

data_before = json.loads(before)
result = sql(migration)
data_after = json.loads(snapshot())
assert 'PASS|fk_member_registered_by|users|t' in result
assert {k: v for k, v in data_before.items() if k != 'constraints'} == {
    k: v for k, v in data_after.items() if k != 'constraints'}
assert sql("SELECT count(*) FROM pg_constraint WHERE conrelid='members'::regclass AND contype='f' "
           "AND conname='fk_member_registered_by' AND confrelid='users'::regclass AND convalidated") == '1'
assert sql("SELECT count(*) FROM pg_constraint WHERE conrelid='members'::regclass AND conname='fk_member_receptionist'") == '0'
check('upgrade_validates_new_fk_preserves_rows_null_history_and_sequence')
before = snapshot()
sql(migration)
assert snapshot() == before
check('replay_and_already_correct_fresh_model_are_noop')

for member_id, creator in [(105, '101'), (106, '102'), (107, 'NULL')]:
    sql(f"BEGIN; INSERT INTO users VALUES({member_id},'Member'); "
        f"INSERT INTO members VALUES({member_id},{creator}); COMMIT;")
    assert sql(f"SELECT coalesce(registered_by::text,'NULL') FROM members WHERE user_id={member_id}") == creator
check('manager_receptionist_and_null_creators_persist_after_commit')
before = snapshot()
sql("BEGIN; INSERT INTO users VALUES(108,'Member'); INSERT INTO members VALUES(108,-391); COMMIT;",
    error='fk_member_registered_by')
assert snapshot() == before
check('unknown_creator_rejected_and_new_user_rolled_back')

# Each incompatible fixture is in the same transaction as migration; failure must preserve the baseline.
guard_cases = [
    ('missing_creator_fk', 'ALTER TABLE members DROP CONSTRAINT fk_member_registered_by;',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('unexpected_name', 'ALTER TABLE members RENAME CONSTRAINT fk_member_registered_by TO custom_creator_fk;',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('unexpected_delete_action', 'ALTER TABLE members DROP CONSTRAINT fk_member_registered_by; '
     'ALTER TABLE members ADD CONSTRAINT fk_member_registered_by FOREIGN KEY(registered_by) REFERENCES users(user_id) ON DELETE CASCADE;',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('unvalidated_creator_fk', 'ALTER TABLE members DROP CONSTRAINT fk_member_registered_by; '
     'ALTER TABLE members ADD CONSTRAINT fk_member_registered_by FOREIGN KEY(registered_by) REFERENCES users(user_id) NOT VALID;',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('duplicate_creator_fk', 'ALTER TABLE members ADD CONSTRAINT extra_creator_fk FOREIGN KEY(registered_by) REFERENCES users(user_id);',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('wrong_column_type', 'ALTER TABLE members ALTER COLUMN registered_by TYPE BIGINT;',
     'SCMS_MEMBER_CREATOR_INCOMPATIBLE'),
    ('orphan_creator', "SET LOCAL session_replication_role=replica; UPDATE members SET registered_by=-391 WHERE user_id=103; SET LOCAL session_replication_role=origin;",
     'SCMS_MEMBER_CREATOR_ORPHAN'),
]
body = migration.replace('BEGIN;', '', 1)
for name, setup, expected in guard_cases:
    sql('BEGIN; ' + setup + body, error=expected)
    assert snapshot() == before, name
    check(name + '_blocks_and_rolls_back')

proof = {'status': 'PASS', 'database': args.database, 'localOnly': True, 'envLoaded': False,
         'remoteWrites': False, 'scope': 'SQL migration/constraint/commit/rollback; no HTTP or provider calls',
         'migrationSha256': hashlib.sha256(migration_path.read_bytes()).hexdigest(), 'checks': checks}
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(proof, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': 'PASS', 'database': args.database, 'checks': len(checks), 'remoteWrites': False}))
