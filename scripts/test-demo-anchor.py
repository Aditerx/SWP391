"""Offline acceptance of anchors/plans/staging; no DB, .env or provider access."""
from pathlib import Path
from datetime import date, datetime, timedelta
import contextlib
import hashlib
import io
import json
import runpy
import subprocess
import sys
import unittest
import uuid

from demo_anchor import anchor_date, holiday_catalog, demo_time, scenario_plan, sql_anchor_guard, VN
from release_catalog import load_requirements, PROFILE

ROOT = Path(__file__).resolve().parents[1]


def design(anchor):
    old = sys.argv
    try:
        sys.argv = ['build-system-data.py','--anchor',str(anchor),'--no-render']
        with contextlib.redirect_stdout(io.StringIO()):
            return runpy.run_path(str(ROOT/'scripts/build-system-data.py'))['tables']
    finally:
        sys.argv = old


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class AnchorAcceptance(unittest.TestCase):
    def test_annual_catalog_survives_reset_design_without_activating_closures(self):
        rows=design(date(2026,11,14))['holidays']
        self.assertEqual(load_requirements(ROOT,PROFILE)['holidayDates'],sorted(r['date'] for r in rows if not r['is_active']))
        self.assertEqual(['2027-01-03'],[r['date'] for r in rows if r['is_active']])

    def test_holiday_catalog_rejects_partial_rows_and_duplicates(self):
        source=(ROOT/'database/seed_holidays_catalog_2026.sql').read_text(encoding='utf-8')
        for broken in [source.replace("DATE '2026-01-01'", "DATE 'bad-date'"),
                       source.replace("DATE '2026-01-01'", "DATE '2026-02-16'")]:
            with self.assertRaises(ValueError): holiday_catalog(broken)

    def test_explicit_date_and_offset_validation(self):
        for value in ['2026-2-01','10/11/2026','2026-02-30','2026-11-10T00:00','2026-11-10\n']:
            with self.assertRaises(ValueError): anchor_date(value)
        anchor=anchor_date('2026-11-11')
        self.assertEqual('2026-11-11T00:30:00+07:00',demo_time('2026-11-10T17:30:00+00:00',anchor).isoformat())
        for value in ['2026-11-11T10:00','2026-11-10T10:00+07:00']:
            with self.assertRaises(ValueError):demo_time(value,anchor)
        self.assertEqual(date(2026,11,12),demo_time('2026-11-12T10:00+07:00',anchor).date())

    def test_design_matrix_weekdays_month_year_and_leap_day(self):
        paths=[ROOT/'SYSTEM-DATA.md',ROOT/'database/schema_postgres.sql',ROOT/'database/bootstrap/scms_demo_scenarios.sql']
        before=[digest(p) for p in paths]
        anchors=[date(2026,11,10)+timedelta(days=i) for i in range(7)]
        anchors += [date(2026,11,30),date(2026,12,31),date(2027,1,1),date(2028,2,29)]
        for anchor in anchors:
            with self.subTest(anchor=str(anchor)):
                tables=design(anchor)
                plan=scenario_plan(tables,anchor,datetime.combine(anchor,datetime.min.time(),VN))
                courses={c['classKey']:c for c in plan['classes']}
                self.assertEqual((1,2,3),(courses['N01']['paidRegisteredBeforeDemo'],courses['N05']['paidRegisteredBeforeDemo'],courses['N11']['paidRegisteredBeforeDemo']))
                self.assertTrue(all(c['executionStatus']=='NOT_RUN' for c in plan['cases']))
                self.assertTrue(all('email' not in actor and 'password' not in actor for actor in plan['actors']))
                slots={(r['coach'],r['slot_key']) for r in tables['coach_work_slots']}
                times={'06:00':1,'07:45':2,'09:30':3,'13:00':4,'14:45':5,'16:30':6,'18:15':7}
                for s in tables['sessions']:
                    if s['status']=='Cancelled':continue
                    day=date.fromisoformat(s['date'])
                    self.assertIn((s['coach'],day.strftime('%A').upper()+':'+str(times[s['start']])),slots)
                rf=next(r for r in tables['payment_resolutions'] if r['key']=='RF04')
                self.assertEqual(str(anchor+timedelta(days=7))+'T23:59:59.999',rf['transfer_due_at'])
        self.assertEqual(before,[digest(p) for p in paths])

    def test_training_results_require_explicit_valid_state_before_sql_generation(self):
        old = sys.argv
        try:
            sys.argv = ['build-system-data.py', '--anchor', '2026-11-10', '--no-render']
            with contextlib.redirect_stdout(io.StringIO()):
                namespace = runpy.run_path(str(ROOT/'scripts/build-system-data.py'))
        finally:
            sys.argv = old
        row = namespace['tables']['training_results'][0]
        original = row['attendance_status']
        missing = object()
        try:
            namespace['validate']()  # The approved, explicit fixture still validates.
            for invalid in [missing, None, '', 'CheckedIn']:
                with self.subTest(state='missing' if invalid is missing else invalid):
                    if invalid is missing:
                        row.pop('attendance_status', None)
                    else:
                        row['attendance_status'] = invalid
                    with self.assertRaisesRegex(AssertionError, 'TRAINING_RESULT_STATE_REQUIRED'):
                        namespace['validate']()
        finally:
            row['attendance_status'] = original
        namespace['validate']()

    def test_weekday_shift_does_not_claim_late_window_is_ready(self):
        anchor=date(2026,11,11);tables=design(anchor)
        plan=scenario_plan(tables,anchor,demo_time('2026-11-11T10:00:00+07:00',anchor))
        n02=next(c for c in plan['classes'] if c['classKey']=='N02')
        self.assertEqual('2026-11-13T06:00:00+07:00',n02['firstSession'])
        self.assertEqual('OUTSIDE_WINDOW',next(c for c in plan['cases'] if c['case']=='DEMO-05_LATE_CONFIRM_N02')['availability'])
        for at,expected in [('2026-11-12T05:59:59+07:00','OUTSIDE_WINDOW'),('2026-11-12T06:00:00+07:00','AVAILABLE'),('2026-11-13T06:00:00+07:00','OUTSIDE_WINDOW')]:
            plan=scenario_plan(tables,anchor,demo_time(at,anchor))
            self.assertEqual(expected,next(c for c in plan['cases'] if c['case']=='DEMO-05_LATE_CONFIRM_N02')['availability'])

    def test_cash_and_transfer_cutoff_is_exclusive(self):
        anchor=date(2026,11,10);tables=design(anchor)
        first={c['key']:datetime.fromisoformat(c['first_session']).replace(tzinfo=VN) for c in tables['classes'] if c['type']=='Normal'}
        for case,key in [('DEMO-02_CASH_N01','N01'),('DEMO-08_TRANSFER','N11')]:
            cutoff=first[key]-timedelta(hours=48)
            for at,expected in [(cutoff-timedelta(microseconds=1),'AVAILABLE'),(cutoff,'OUTSIDE_WINDOW')]:
                plan=scenario_plan(tables,anchor,at)
                self.assertEqual(expected,next(c for c in plan['cases'] if c['case']==case)['availability'])

    def test_no_demo_time_keeps_windows_unevaluated_and_case_ids_distinct(self):
        anchor=date(2026,11,11);plan=scenario_plan(design(anchor),anchor)
        self.assertIsNone(plan['plannedDemoAt'])
        self.assertEqual(10,len(plan['cases']))
        self.assertTrue(all(c['availability']=='NOT_EVALUATED' and c['executionStatus']=='NOT_RUN' for c in plan['cases']))
        self.assertTrue(all(c['availableUntilExclusive'] for c in plan['cases'] if c['case'] in ['DEMO-02_CASH_N01','DEMO-05_LATE_CONFIRM_N02']))
        self.assertEqual([f'DEMO-{i:02}' for i in range(1,11)],[c['case'].split('_')[0] for c in plan['cases']])

    def test_optional_demo_time_does_not_change_sql_or_fixture(self):
        base=ROOT/'artifacts/runs'/('anchor-optional-'+uuid.uuid4().hex[:10])
        outputs=[]
        for suffix,time_args in [('unknown',[]),('known',['--demo-at','2026-11-11T10:00:00+07:00'])]:
            output=base/suffix;outputs.append(output)
            r=subprocess.run([sys.executable,str(ROOT/'scripts/prepare-demo-bundle.py'),'--anchor','2026-11-11',
                              '--output-dir',str(output),*time_args],cwd=ROOT,capture_output=True,text=True,encoding='utf-8')
            self.assertEqual(0,r.returncode,r.stderr[-1000:])
            plan=json.loads((output/'demo-plan.json').read_text(encoding='utf-8'))
            if not time_args:
                self.assertIsNone(plan['plannedDemoAt'])
                self.assertTrue(all(c['availability']=='NOT_EVALUATED' for c in plan['cases']))
        for filename in ['schema_postgres.sql','scms_demo_scenarios.sql']:
            self.assertEqual(digest(outputs[0]/filename),digest(outputs[1]/filename))

    def test_staged_generation_is_reproducible_and_leaves_canonical_untouched(self):
        canonical=[ROOT/'database/schema_postgres.sql',ROOT/'database/bootstrap/scms_demo_scenarios.sql',ROOT/'SYSTEM-DATA.md']
        before=[digest(p) for p in canonical]
        base=ROOT/'artifacts/runs'/('anchor-offline-'+uuid.uuid4().hex[:10])
        outputs=[]
        for name in ['a','b']:
            output=base/name;outputs.append(output)
            r=subprocess.run([sys.executable,str(ROOT/'scripts/prepare-demo-bundle.py'),'--anchor','2026-11-11',
                              '--demo-at','2026-11-11T10:00:00+07:00','--output-dir',str(output)],cwd=ROOT,capture_output=True,text=True,encoding='utf-8')
            self.assertEqual(0,r.returncode,r.stderr[-1200:])
            sql=(output/'schema_postgres.sql').read_text(encoding='utf-8')
            self.assertLess(sql.index('SCMS_DEMO_STALE_ANCHOR'),sql.index('CREATE SCHEMA'))
            self.assertLess(sql.index('SCMS_DEMO_STALE_ANCHOR'),sql.index('DROP TABLE'))
            proof=json.loads((output/'verification.json').read_text(encoding='utf-8'))
            self.assertEqual('NOT_RUN',proof['httpBusiness']['status'])
            self.assertFalse(proof['cloudExecuted'])
        for name in ['schema_postgres.sql','scms_demo_scenarios.sql','demo-plan.json']:
            self.assertEqual(digest(outputs[0]/name),digest(outputs[1]/name),name)
        self.assertEqual(before,[digest(p) for p in canonical])

    def test_missing_anchor_unsafe_output_and_db_names_fail_without_generation(self):
        for script,params in [('build-demo-schema.py',[]),('build-system-data.py',['--no-render']),
                              ('prepare-demo-bundle.py',['--anchor','2026-11-10','--demo-at','2026-11-10T10:00:00+07:00','--output-dir','database']),
                              ('prepare-demo-bundle.py',['--anchor','2026-11-10','--demo-at','2026-11-10T10:00:00+07:00','--output-dir','artifacts/runs/invalid-anchor-test','--verify-local','--database','postgres'])]:
            r=subprocess.run([sys.executable,str(ROOT/'scripts'/script),*params],cwd=ROOT,capture_output=True,text=True,encoding='utf-8')
            self.assertEqual(2,r.returncode,(script,r.stderr[-500:]))
        guard=sql_anchor_guard(date(2026,11,10))
        self.assertIn("clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh'",guard)
        self.assertNotIn('CURRENT_DATE',guard)


if __name__=='__main__':unittest.main(verbosity=2)
