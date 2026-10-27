"""Local proof for registration OTP, chat and notification using real HTTP/PostgreSQL/SMTP.

Creates a NEW loopback-only PostgreSQL container, keeps its data and stops it on exit.
Never reads .env, never connects Supabase, never forwards mail to external recipients.
Requires Docker Desktop, Java 17 and the Maven wrapper. Run from any directory:
    python scripts/test-communication-features.py
Evidence is explicitly local integration, not Gmail delivery, FE browser or deployment.
"""
from pathlib import Path
import datetime as dt
import hashlib
import json
import os
import re
import socket
import subprocess
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET

SOURCE_ROOT = Path(__file__).resolve().parents[1]
ROOT = SOURCE_ROOT
RUN = dt.datetime.now(dt.timezone(dt.timedelta(hours=7))).strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6]
CONTAINER = "scms-communication-proof-" + RUN
EVIDENCE = SOURCE_ROOT / "artifacts" / "communication-proof" / RUN
LOGS = Path(tempfile.mkdtemp(prefix="scms-communication-"))
LEGACY_SCHEMA = "fc00c6499547a2873da6787326d508140e24b0fd:database/schema_postgres.sql"
EVIDENCE.mkdir(parents=True)
manifest = {"run_id": RUN, "evidence_type": "LOCAL HTTP + PostgreSQL + SMTP sockets",
            "source_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
            "source_dirty": bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT)),
            "container": CONTAINER, "raw_log_directory": str(LOGS), "databases": [],
            "env_read_by_runner": False, "cloud_database_touched": False,
            "external_email_sent": False, "automatic_jobs_disabled": True,
            "not_verified": ["Gmail delivery", "FE browser", "Render", "all scheduled job executions"],
            "result": "RUNNING"}


def command(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, capture_output=True, text=True, encoding="utf-8", **kwargs)


def sql(database, content):
    assert re.fullmatch(r"scms_communication_(fresh|upgrade)|postgres", database)
    return command(["docker", "exec", "-i", CONTAINER, "psql", "-X", "-A", "-t", "-v", "ON_ERROR_STOP=1",
                    "-U", "postgres", "-d", database], input=content).stdout.strip()


def snapshot(database):
    return sql(database, """SELECT md5(string_agg(row_to_json(t)::text, ',' ORDER BY invoice_id)) FROM invoices t;
        SELECT md5(string_agg(row_to_json(t)::text, ',' ORDER BY subscription_id)) FROM member_packages t;
        SELECT md5(string_agg(row_to_json(t)::text, ',' ORDER BY session_id)) FROM sessions t;""")


started = False
try:
    # Use committed sources in a checkout without .env or another Maven target.
    source = manifest["source_commit"]
    checkout = LOGS / "checkout"
    command(["git", "worktree", "add", "--detach", str(checkout), source])
    ROOT = checkout
    assert not (ROOT / ".env").exists(), "Isolated proof must never load production configuration"
    manifest["isolated_checkout"] = str(checkout)
    manifest["checkout_commit"] = command(["git", "rev-parse", "HEAD"]).stdout.strip()
    command(["docker", "info", "--format", "{{.ServerVersion}}"], timeout=30)
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        port = probe.getsockname()[1]
    # Disposable local-only credentials; these are unrelated to .env or any external service.
    command(["docker", "run", "--detach", "--name", CONTAINER, "--label", "scms.proof=communication",
             "-e", "POSTGRES_PASSWORD=local-proof-only", "-p", f"127.0.0.1:{port}:5432", "postgres:17"], timeout=120)
    started = True
    for attempt in range(30):
        ready = subprocess.run(["docker", "exec", CONTAINER, "pg_isready", "-U", "postgres"], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError("Isolated PostgreSQL was not ready")
    manifest["postgres_version"] = sql("postgres", "SHOW server_version;")
    phase_names = ["07_session_subscription_postgres.sql", "08_advanced_entitlements_postgres.sql",
                   "09_approved_catalog_postgres.sql", "10_session_incidents_postgres.sql",
                   "11_holiday_history_postgres.sql", "12_payment_resolutions_postgres.sql"]
    schema_paths = ["database/migrations/" + name for name in phase_names] + ["database/schema_postgres.sql", "database/migrations/05_migration_notifications.sql",
                    "database/migrations/06_migration_chat.sql", "database/migrations/06_schedule_safety_postgres.sql"]
    manifest["file_sha256"] = {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in schema_paths}
    for mode in ["fresh", "upgrade"]:
        database = "scms_communication_" + mode
        sql("postgres", f"CREATE DATABASE {database};")
        if mode == "fresh":
            schema = (ROOT / "database/schema_postgres.sql").read_text(encoding="utf-8-sig")
        else:
            schema = command(["git", "show", LEGACY_SCHEMA]).stdout
        sql(database, schema)
        sql(database, (ROOT / "database/migrations/06_schedule_safety_postgres.sql").read_text(encoding="utf-8-sig"))
        for name in phase_names:
            sql(database, (ROOT / "database/migrations" / name).read_text(encoding="utf-8-sig"))
        before = snapshot(database)
        if mode == "upgrade":
            sql(database, """INSERT INTO notifications(user_id,title,content,type) VALUES(4,'Legacy proof','Keep original content','System');
                INSERT INTO notification_recipients(notification_id,user_id,is_read)
                SELECT notification_id,4,TRUE FROM notifications WHERE title='Legacy proof';""")
        for repeat in range(2):
            for name in ["05_migration_notifications.sql", "06_migration_chat.sql"]:
                sql(database, (ROOT / "database/migrations" / name).read_text(encoding="utf-8-sig"))
        assert snapshot(database) == before, "Feature migrations changed invoice/subscription/session snapshot"
        if mode == "upgrade":
            assert sql(database, "SELECT count(*) FROM notifications n JOIN notification_recipients r USING(notification_id) WHERE n.title='Legacy proof' AND n.content='Keep original content' AND r.is_read=TRUE;") == "1"
        entry = {"name": database, "schema_mode": mode, "repeat_migrations": "PASS", "business_snapshot_unchanged": True}
        manifest["databases"].append(entry)
        folder = EVIDENCE / mode
        folder.mkdir()
        args = [str(ROOT / "mvnw.cmd"), "-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh", "-Dtest=CommunicationFeaturesPostgresTest",
                "-Dscms.communication.proof=true", f"-Dscms.communication.db-url=jdbc:postgresql://127.0.0.1:{port}/{database}",
                f"-Dscms.communication.evidence-dir={folder}", "test"]
        logfile = LOGS / (mode + "-maven.log")
        print(f"Running {mode}: real HTTP sessions, PostgreSQL and loopback SMTP", flush=True)
        with logfile.open("w", encoding="utf-8") as output:
            result = subprocess.run(args, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
        entry["command"] = args
        entry["exit_code"] = result.returncode
        entry["raw_log_sha256"] = hashlib.sha256(logfile.read_bytes()).hexdigest()
        report = ROOT / "target/surefire-reports/TEST-com.sportscenter.communication.CommunicationFeaturesPostgresTest.xml"
        if report.exists():
            suite = ET.parse(report).getroot()
            entry["tests"] = {key: int(suite.attrib.get(key, 0)) for key in ["tests", "failures", "errors", "skipped"]}
            entry["cases"] = [{"name": case.attrib["name"],
                               "result": "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"}
                              for case in suite.findall("testcase")]
        if result.returncode:
            raise RuntimeError(f"{mode} integration failed; inspect sanitized step evidence and raw local log at {logfile}")
        assert entry["tests"] == {"tests": 6, "failures": 0, "errors": 0, "skipped": 0}, "Expected all six integration cases to execute"
        print(f"PASS {mode}: six application integration cases, migration repeat and data snapshots", flush=True)
    manifest["result"] = "PASS"
except Exception as exc:
    manifest["result"] = "FAIL"
    manifest["failure"] = str(exc)
    if isinstance(exc, subprocess.CalledProcessError):
        (LOGS / "setup-error.log").write_text((exc.stdout or "") + (exc.stderr or ""), encoding="utf-8")
    raise
finally:
    if started:
        stopped = subprocess.run(["docker", "stop", CONTAINER], capture_output=True)
        manifest["container_stopped_data_retained"] = stopped.returncode == 0
    (EVIDENCE / "verification.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("Evidence:", EVIDENCE, flush=True)
