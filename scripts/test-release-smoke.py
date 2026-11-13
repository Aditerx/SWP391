"""Offline transport contract tests. No credentials, cloud or business writes."""
import importlib.util
import io
from pathlib import Path
import ssl
import json
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import urllib.error
import urllib.request

spec = importlib.util.spec_from_file_location('release_smoke', Path(__file__).with_name('release-smoke.py'))
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class FakeTime:
    value = 0
    def now(self): return self.value
    def sleep(self, delay): self.value += delay


class Response:
    status = 200
    headers = {'Content-Type': 'application/json'}
    def __init__(self, body=b'{}'): self.body = body; self.closed = False
    def __enter__(self): return self
    def __exit__(self, *args): self.closed = True
    def read(self):
        if isinstance(self.body, Exception): raise self.body
        return self.body


class Opener:
    def __init__(self, actions, clock): self.actions = iter(actions); self.calls = []; self.clock = clock
    def open(self, request, timeout):
        self.calls.append((request.get_method(), timeout))
        action = next(self.actions)
        if callable(action): action = action(timeout)
        if isinstance(action, Exception): raise action
        return action


class StartupRetryTest(unittest.TestCase):
    def fetch(self, actions, method='GET', path='/v3/api-docs', **kwargs):
        self.clock = FakeTime(); self.events = []
        self.opener = Opener(actions, self.clock)
        req = urllib.request.Request('http://127.0.0.1:12345' + path, method=method)
        return smoke.fetch_with_startup_retry(self.opener, req,
            lambda *event: self.events.append(event), monotonic=self.clock.now,
            sleep=self.clock.sleep, **kwargs)

    def test_timeout_then_success_retains_first_failure(self):
        self.assertEqual(200, self.fetch([TimeoutError(), Response()])[0])
        self.assertEqual(['RETRY', 'SUCCESS'], [e[1] for e in self.events])
        self.assertEqual('TimeoutError', self.events[0][2])
        self.assertEqual(2, len(self.opener.calls))

    def test_body_read_timeout_retries_and_closes_response(self):
        failed = Response(TimeoutError())
        self.assertEqual(200, self.fetch([failed, Response()])[0])
        self.assertTrue(failed.closed)

    def test_persistent_timeout_exhausts_three_attempts(self):
        with self.assertRaises(smoke.StartupReadExhausted): self.fetch([TimeoutError()] * 3)
        self.assertEqual(3, len(self.opener.calls))
        self.assertEqual(['RETRY', 'RETRY', 'EXHAUSTED'], [e[1] for e in self.events])

    def test_deadline_caps_remaining_timeout(self):
        def consume(timeout): self.clock.value += timeout; return TimeoutError()
        with self.assertRaises(smoke.StartupReadExhausted):
            self.fetch([consume] * 3, timeout=35, deadline=50)
        self.assertEqual([35, 13], [t for _, t in self.opener.calls])
        self.assertEqual(50, self.clock.value)

    def test_too_short_deadline_does_not_sleep_or_repeat(self):
        with self.assertRaises(smoke.StartupReadExhausted):
            self.fetch([TimeoutError()], deadline=1)
        self.assertEqual(1, len(self.opener.calls))
        self.assertEqual(0, self.clock.value)

    def test_only_gateway_startup_statuses_retry(self):
        for code in (502, 503, 504):
            with self.subTest(code=code):
                error = urllib.error.HTTPError('http://127.0.0.1', code, '', {}, io.BytesIO())
                self.assertEqual(200, self.fetch([error, Response()])[0])
                self.assertEqual('HTTP_' + str(code), self.events[0][2])
        for code in (400, 401, 403, 404, 409, 429, 500):
            with self.subTest(code=code):
                error = urllib.error.HTTPError('http://127.0.0.1', code, '', {}, io.BytesIO())
                with self.assertRaises(urllib.error.HTTPError): self.fetch([error])
                self.assertEqual(1, len(self.opener.calls))
                self.assertEqual([], self.events)
                error.close()

    def test_login_logout_and_mutations_never_retry(self):
        for method, path in [('POST', '/api/auth/login'), ('POST', '/api/auth/logout'),
                             ('PUT', '/api/roles/1/permissions'), ('POST', '/api/rooms'),
                             ('PATCH', '/api/training-results/1/content'),
                             ('POST', '/api/health'), ('GET', '/api/training-results')]:
            with self.subTest(method=method, path=path):
                with self.assertRaises(TimeoutError): self.fetch([TimeoutError()], method, path)
                self.assertEqual(1, len(self.opener.calls))
                self.assertEqual([], self.events)

    def test_mutation_gateway_status_does_not_retry(self):
        for code in (502, 503, 504):
            error = urllib.error.HTTPError('http://127.0.0.1', code, '', {}, io.BytesIO())
            with self.assertRaises(urllib.error.HTTPError): self.fetch([error], 'POST', '/api/auth/login')
            self.assertEqual(1, len(self.opener.calls))
            error.close()

    def test_readiness_transport_allowlist(self):
        for path in ('/api/health', '/actuator/health', '/v3/api-docs'):
            for error in (urllib.error.URLError(TimeoutError()), ConnectionResetError()):
                with self.subTest(path=path, error=type(error).__name__):
                    self.assertEqual(200, self.fetch([error, Response()], path=path)[0])

    def test_tls_configuration_error_is_not_a_startup_retry(self):
        error = urllib.error.URLError(ssl.SSLCertVerificationError('test'))
        with self.assertRaises(urllib.error.URLError): self.fetch([error])
        self.assertEqual(1, len(self.opener.calls))

    def test_late_success_cannot_pass_readiness_deadline(self):
        def late(timeout): self.clock.value += timeout + 1; return Response()
        with self.assertRaises(smoke.StartupReadExhausted): self.fetch([late], deadline=10)
        self.assertEqual('EXHAUSTED', self.events[0][1])

    def test_report_distinguishes_startup_exhaustion_and_api_assertion(self):
        for actions, expected in [([TimeoutError()] * 3, 'STARTUP_TRANSPORT_EXHAUSTED'),
                                  ([Response(b'{"status":"UP","buildCommit":"wrong"}')], 'API_ASSERTION')]:
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as directory:
                clock = FakeTime(); opener = Opener(actions, clock)
                original = smoke.fetch_with_startup_retry
                def fetch(*args, **kwargs):
                    return original(*args, **kwargs, monotonic=clock.now, sleep=clock.sleep)
                output = Path(directory) / 'result.json'
                args = SimpleNamespace(base_url='http://127.0.0.1:12345', phase='post-deploy',
                    expected_commit='candidate', expected_db_fingerprint='db', negative_mutations=False,
                    request_timeout=35, startup_deadline=110, output=str(output))
                with patch.object(smoke.urllib.request, 'build_opener', return_value=opener), \
                        patch.object(smoke, 'fetch_with_startup_retry', side_effect=fetch), \
                        patch('builtins.print'):
                    self.assertEqual(1, smoke.run(args))
                report = json.loads(output.read_text())
                self.assertEqual(expected, report['failure']['category'])
                self.assertEqual([], report['authenticatedActors'])
                self.assertEqual('/api/health', report['failure']['request']['path'])
                if expected == 'STARTUP_TRANSPORT_EXHAUSTED':
                    self.assertEqual('TimeoutError', report['firstStartupFailure']['reason'])
                    self.assertEqual(3, len(report['startupAttempts']))


if __name__ == '__main__': unittest.main()
