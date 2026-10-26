"""Exercise F06/F07 SQL on disposable Docker PostgreSQL; never uses .env or live DB.
Usage: python scripts/test-session-subscription-postgres.py
Requires container scms-f0607-postgres on 127.0.0.1:55439 (see deployment guide).
Use --container scms-f0607-<unique-name> for a fresh container without replacing previous test data.
"""
from pathlib import Path
import argparse
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--container", default="scms-f0607-postgres")
CONTAINER = parser.parse_args().container
if not re.fullmatch(r"scms-f0607-[A-Za-z0-9][A-Za-z0-9_.-]*", CONTAINER):
    parser.error("Only an isolated scms-f0607-* test container is allowed")
MIGRATION = (ROOT / "database/migrations/07_session_subscription_postgres.sql").read_text(encoding="utf-8")

for attempt in range(30):
    ready = subprocess.run(["docker", "exec", CONTAINER, "pg_isready", "-U", "postgres"],
                           capture_output=True)
    if ready.returncode == 0:
        break
    time.sleep(1)
else:
    raise RuntimeError("Disposable PostgreSQL was not ready within 30 seconds")


def sql(database, content, expect_success=True, expected_error=None):
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "psql", "-X", "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d", database],
        input=content, text=True, encoding="utf-8", capture_output=True, cwd=ROOT)
    if (result.returncode == 0) != expect_success:
        raise RuntimeError(result.stdout + result.stderr)
    if expected_error is not None and expected_error not in result.stderr:
        raise AssertionError(result.stderr)
    return result.stdout


def snapshot(database):
    return sql(database, """SELECT md5(string_agg((to_jsonb(s)-'subscription_id')::text, ',' ORDER BY session_id)) FROM sessions s;
        SELECT md5(string_agg(row_to_json(mp)::text, ',' ORDER BY subscription_id)) FROM member_packages mp;
        SELECT md5(string_agg(row_to_json(i)::text, ',' ORDER BY invoice_id)) FROM invoices i;""")


for suffix, schema_path in [("fresh", "schema_postgres.sql"), ("upgrade", "database/schema_postgres.sql")]:
    db = "scms_f0607_" + suffix
    # CREATE only: rerunning requires a new disposable container, never deletes a database.
    sql("postgres", f"CREATE DATABASE {db};")
    schema = (ROOT / schema_path).read_text(encoding="utf-8")
    if suffix == "upgrade":
        # Reproduce the pre-fix schema without relying on a particular Git commit.
        schema = schema.replace("    subscription_id      INT NULL,\n", "", 1).replace(MIGRATION, "", 1)
    sql(db, schema)
    sql(db, (ROOT / "database/migrations/06_schedule_safety_postgres.sql").read_text(encoding="utf-8"))
    original = snapshot(db)
    if suffix == "upgrade":
        sql(db, "SELECT subscription_id FROM sessions LIMIT 0;", expect_success=False,
            expected_error='column "subscription_id" does not exist')
        sql(db, MIGRATION)
    assert original == snapshot(db), "Upgrade changed existing business data"
    assert "0" in sql(db, "SELECT count(*) FROM sessions WHERE subscription_id IS NOT NULL;")
    # Valid non-null association is preserved too; this is disposable test data only.
    sql(db, """UPDATE sessions SET subscription_id=(SELECT min(subscription_id) FROM member_packages)
        WHERE session_id=(SELECT min(session_id) FROM sessions);""")
    link = sql(db, "SELECT session_id,subscription_id FROM sessions ORDER BY session_id;")
    before = snapshot(db)
    sql(db, MIGRATION)
    assert before == snapshot(db), "Migration changed business data"
    assert link == sql(db, "SELECT session_id,subscription_id FROM sessions ORDER BY session_id;")
    # An equivalent FK/index under other names must be reused, including NOT VALID FK.
    sql(db, """ALTER TABLE sessions RENAME CONSTRAINT fk_session_subscription TO custom_subscription_fk;
        ALTER INDEX idx_sessions_subscription RENAME TO custom_subscription_idx;""")
    sql(db, MIGRATION)
    assert "fk_session_subscription" not in sql(db, "SELECT conname FROM pg_constraint WHERE conrelid='sessions'::regclass;")
    assert "idx_sessions_subscription" not in sql(db, "SELECT indexname FROM pg_indexes WHERE tablename='sessions';")
    assert before == snapshot(db)
    sql(db, """ALTER TABLE sessions DROP CONSTRAINT custom_subscription_fk;
        ALTER TABLE sessions ADD CONSTRAINT custom_subscription_fk
        FOREIGN KEY (subscription_id) REFERENCES member_packages(subscription_id) NOT VALID;""")
    sql(db, MIGRATION)
    assert "t" in sql(db, "SELECT convalidated FROM pg_constraint WHERE conname='custom_subscription_fk';")
    # Uncommitted deliberately bad fixtures: migration must fail and roll back everything.
    orphan = """BEGIN; ALTER TABLE sessions DROP CONSTRAINT custom_subscription_fk;
        UPDATE sessions SET subscription_id = -999 WHERE session_id=(SELECT min(session_id) FROM sessions);
        """ + MIGRATION.replace("BEGIN;", "", 1)
    sql(db, orphan, expect_success=False, expected_error="Orphan sessions.subscription_id detected")
    assert before == snapshot(db), "Orphan preflight modified persisted data"
    collision = """BEGIN; DROP INDEX custom_subscription_idx;
        CREATE INDEX idx_sessions_subscription ON sessions(room_id);
        """ + MIGRATION.replace("BEGIN;", "", 1)
    sql(db, collision, expect_success=False, expected_error="Index name idx_sessions_subscription already used")
    assert before == snapshot(db)
    incompatible = """BEGIN; ALTER TABLE sessions DROP CONSTRAINT custom_subscription_fk;
        ALTER TABLE sessions ADD CONSTRAINT custom_subscription_fk FOREIGN KEY (subscription_id)
        REFERENCES member_packages(subscription_id) ON DELETE CASCADE;
        """ + MIGRATION.replace("BEGIN;", "", 1)
    sql(db, incompatible, expect_success=False, expected_error="Incompatible subscription foreign key")
    assert before == snapshot(db)
    print(f"PASS {db}: schema, repeat, renamed definitions, orphan/collision rollback, data unchanged", flush=True)
    subprocess.run([str(ROOT / "mvnw.cmd"), "-Dtest=SessionSchemaPostgresTest",
                    "-Duser.timezone=Asia/Ho_Chi_Minh",
                    "-Dscms.postgres.schema-test=true", f"-Dscms.postgres.test-url=jdbc:postgresql://127.0.0.1:55439/{db}",
                    "test"], cwd=ROOT, check=True, stderr=subprocess.STDOUT)
