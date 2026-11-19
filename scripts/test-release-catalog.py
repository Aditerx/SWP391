"""Reference catalog regression cases, especially missing/inactive holiday lists."""
import copy
from pathlib import Path
import tempfile
import unittest
from release_catalog import PROFILE, assess_bundle_holidays, assess_catalogs, load_requirements, read_catalogs


class CatalogGateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = Path(__file__).resolve().parents[1]
        cls.requirements = load_requirements(cls.root, PROFILE)

    def setUp(self):
        self.data = {
            'subjects': [dict(id=i, slug=key, status='Active')
                         for i, key in enumerate(self.requirements['subjectSlugs'], 1)],
            'rooms': [dict(id=i, roomCode=key, status='Available')
                      for i, key in enumerate(self.requirements['roomCodes'], 1)],
            'holidays': [dict(id=i, holidayDate=key, active=False)
                         for i, key in enumerate(self.requirements['holidayDates'], 1)],
            'activeHolidays': [], 'classes': [],
        }

    def assess(self): return assess_catalogs(self.data, self.requirements)

    def test_profile_covers_all_reviewed_source_rows(self):
        self.assertEqual([9, 20, 11], [len(self.requirements[k]) for k in
                                     ('subjectSlugs', 'roomCodes', 'holidayDates')])

    def test_inactive_catalog_with_empty_active_list_is_complete(self):
        result = self.assess()
        self.assertEqual('PASS', result['status'])
        self.assertEqual(11, result['observed']['inactiveHolidayCount'])
        self.assertEqual('NOT_RUN', result['businessFixtureReadiness'])

    def test_empty_holiday_list_fails_even_when_http200_and_active_empty(self):
        self.data['holidays'] = []
        result = self.assess()
        self.assertEqual('FAIL', result['status'])
        self.assertEqual(self.requirements['holidayDates'], result['missingRequiredKeys']['holidays'])

    def test_bundle_only_demo_holiday_does_not_prove_annual_catalog(self):
        self.data['holidays'] = [dict(id=1, holidayDate='2027-01-01', active=True)]
        self.data['activeHolidays'] = copy.deepcopy(self.data['holidays'])
        self.assertEqual(11, len(self.assess()['missingRequiredKeys']['holidays']))

    def test_same_count_with_wrong_date_fails_membership(self):
        missing = self.data['holidays'][0]['holidayDate']
        self.data['holidays'][0]['holidayDate'] = '2026-01-03'
        self.assertEqual([missing], self.assess()['missingRequiredKeys']['holidays'])

    def test_all_endpoint_cannot_be_replaced_by_active_endpoint(self):
        self.data['holidays'] = []
        self.assertEqual('FAIL', self.assess()['status'])

    def test_active_endpoint_agrees_with_all_flags_but_flags_are_not_frozen(self):
        self.data['holidays'][0]['active'] = True
        self.data['activeHolidays'] = [copy.deepcopy(self.data['holidays'][0])]
        self.assertEqual('PASS', self.assess()['status'])
        self.data['activeHolidays'] = []
        self.assertEqual('FAIL', self.assess()['status'])

    def test_missing_subject_or_room_fails_without_requiring_exact_counts(self):
        for key in ('subjects', 'rooms'):
            with self.subTest(key=key):
                row = self.data[key].pop()
                self.assertEqual('FAIL', self.assess()['status'])
                self.assertIn(key, self.assess()['missingRequiredKeys'])
                self.data[key].append(row)

    def test_extra_catalog_entries_and_authorized_status_changes_are_allowed(self):
        self.data['holidays'].append(dict(id=100, holidayDate='2027-01-01', active=True))
        self.data['activeHolidays'] = [copy.deepcopy(self.data['holidays'][-1])]
        self.data['rooms'][0]['status'] = 'Maintenance'
        self.data['subjects'][0]['status'] = 'Inactive'
        self.assertEqual('PASS', self.assess()['status'])

    def test_no_profile_is_not_completeness_pass(self):
        self.assertEqual('NOT_RUN', assess_catalogs(self.data, None)['status'])
        self.assertIsNone(load_requirements(self.root, None))

    def test_bad_json_shapes_ids_flags_dates_and_duplicates_fail(self):
        cases = [('holidays', {}), ('subjects', {'content': []}),
                 ('holidays', [dict(id={}, holidayDate='2026-01-01', active=False)]),
                 ('holidays', [dict(id=1, holidayDate='wrong', active=False)]),
                 ('holidays', [dict(id=1, holidayDate='2026-01-01', active='false')]),
                 ('classes', [dict(id=1)]),
                 ('rooms', [dict(id=1, roomCode='DUP', status='Available')] * 2)]
        for key, value in cases:
            with self.subTest(key=key, value=value):
                before = self.data[key]; self.data[key] = value
                self.assertEqual('FAIL', self.assess()['status'])
                self.data[key] = before

    def test_missing_source_and_unknown_profile_cannot_silently_pass(self):
        with self.assertRaises(ValueError): load_requirements(self.root, 'unknown')
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError): load_requirements(directory, PROFILE)

    def test_reads_only_catalog_gets_and_saves_no_business_payload(self):
        paths = []
        mapping = dict(zip(['/api/subjects', '/api/rooms', '/api/holidays',
                            '/api/holidays/active', '/api/classes'], self.data.values()))
        def request(alias, method, path, **kwargs):
            self.assertEqual(('CM', 'GET', (200,)), (alias, method, kwargs['expected']))
            paths.append(path)
            return 200, mapping[path], {}
        result = read_catalogs(request, 'CM', self.requirements)
        self.assertEqual('PASS', result['status'])
        self.assertEqual(list(mapping), paths)
        self.assertNotIn('classes', result['observed'])

    def test_offline_bundle_gate_rejects_missing_catalog_before_any_sql_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            bundle = Path(directory) / 'bundle.sql'
            bundle.write_text("INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2027-01-01','DEMO','CENTER_EVENT',TRUE);")
            report = assess_bundle_holidays(bundle, self.requirements)
            self.assertEqual('FAIL', report['status'])
            self.assertEqual(self.requirements['holidayDates'], report['missingHolidayDates'])
            bundle.write_text('\n'.join("INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('" +
                d + "','Demo','NATIONAL_FIXED',FALSE);" for d in self.requirements['holidayDates']))
            self.assertEqual('PASS', assess_bundle_holidays(bundle, self.requirements)['status'])

    def test_unsupported_bundle_sql_requires_review_instead_of_silent_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            bundle = Path(directory) / 'bundle.sql'
            bundle.write_text('INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) SELECT * FROM demo;')
            self.assertEqual('REVIEW_REQUIRED', assess_bundle_holidays(bundle, self.requirements)['status'])


if __name__ == '__main__': unittest.main()
