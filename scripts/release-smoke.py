"""Bounded SCMS release smoke: identity, fresh DEMO sessions and scoped readback.

No .env, DB connections, reset/seed, payments, provider calls or credential output.
Credentials come from an ignored private JSON file: {"actors":[{"alias":...,
"email":...,"password":...,"role":...}]}. Cookies stay in memory only.
Default mode is read-only apart from login/logout. --negative-mutations adds
invalid/forbidden requests expected to roll back, never a positive money write.
"""
from pathlib import Path
import argparse
import datetime as dt
import hashlib
import http.cookiejar
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


def open_with_startup_retry(opener, request, record_retry, delay=2):
    """Retry only public readiness/schema reads; never repeat login or a mutation."""
    path = urllib.parse.urlsplit(request.full_url).path
    warmup = request.get_method() == 'GET' and path in {
        '/api/health', '/actuator/health', '/v3/api-docs'}
    attempts = 3 if warmup else 1
    for attempt in range(1, attempts + 1):
        try:
            return opener.open(request, timeout=35)
        except urllib.error.HTTPError as error:
            if not warmup or error.code not in (502, 503, 504) or attempt == attempts:
                raise
            error.close()
            record_retry(attempt, 'HTTP_' + str(error.code))
        except (TimeoutError, urllib.error.URLError, ConnectionError) as error:
            if not warmup or attempt == attempts:
                raise
            record_retry(attempt, type(error).__name__)
        time.sleep(delay)


def run(args):
    base = args.base_url.rstrip('/')
    target = urllib.parse.urlsplit(base)
    local = target.scheme == 'http' and target.hostname in ('127.0.0.1', 'localhost')
    if not (local or base == 'https://scms-app-p1gy.onrender.com'):
        raise ValueError('Only the project Render service or loopback is allowed')
    report = {'checkedAt': dt.datetime.now(dt.timezone(dt.timedelta(hours=7))).isoformat(),
              'phase': args.phase, 'baseUrl': base, 'expectedCommit': args.expected_commit,
              'cases': [], 'authenticatedActors': [], 'cloudSqlExecuted': False,
              'providersCalled': False, 'credentialsRecorded': False,
              'negativeMutationsEnabled': args.negative_mutations,
              'limits': ['Bounded HTTP smoke, not every API/input/role combination',
                         'No FE browser, bank, mail inbox, AI or Google provider acceptance']}
    contexts = {}
    checks = report['cases']

    def check(name, passed, **evidence):
        checks.append(dict(case=name, result='PASS' if passed else 'FAIL', **evidence))
        if not passed:
            raise AssertionError(name)

    def request(alias, method, path, body=None, expected=None, headers=None):
        opener = contexts.get(alias, urllib.request.build_opener())
        hdrs = {'User-Agent': 'SCMS-Scoped-Release-Smoke/1.0', 'Accept': 'application/json'}
        hdrs.update(headers or {})
        payload = None if body is None else json.dumps(body).encode('utf-8')
        if payload is not None:
            hdrs['Content-Type'] = 'application/json'
        req = urllib.request.Request(base + path, data=payload, method=method, headers=hdrs)
        try:
            def retry_event(attempt, reason):
                checks.append({'case': method + ' ' + path.split('?')[0] + ':startup read',
                               'result': 'RETRY', 'attempt': attempt, 'reason': reason})
            with open_with_startup_retry(opener, req, retry_event) as response:
                status, raw, response_headers = response.status, response.read(), response.headers
        except urllib.error.HTTPError as error:
            status, raw, response_headers = error.code, error.read(), error.headers
        try:
            value = json.loads(raw)
        except (ValueError, UnicodeDecodeError):
            value = None
        if expected is not None:
            check(alias + ':' + method + ' ' + path.split('?')[0], status in expected,
                  http=status, expected=list(expected), responseSha256=hashlib.sha256(raw).hexdigest())
        return status, value, response_headers

    try:
        _, health, _ = request('public', 'GET', '/api/health', expected=(200,))
        check('runtime identity', health.get('status') == 'UP'
              and health.get('buildCommit') == args.expected_commit,
              observedCommit=health.get('buildCommit'))
        report['runtimeIdentity'] = {k: health.get(k) for k in
                                     ['buildCommit', 'databaseTargetFingerprint', 'diagnosticsVersion']}
        if args.expected_db_fingerprint:
            check('same database target', health.get('databaseTargetFingerprint') == args.expected_db_fingerprint)
        _, actuator, _ = request('public', 'GET', '/actuator/health', expected=(200,))
        check('dependency health', actuator.get('status') == 'UP')
        _, spec, _ = request('public', 'GET', '/v3/api-docs', expected=(200,))
        report['pathCount'] = len(spec.get('paths', {}))
        if args.phase == 'post-deploy':
            for path, method in [('/api/training-results/{resultId}/content', 'patch'),
                                 ('/api/chat/conversations/{id}/close', 'post'),
                                 ('/api/roles/{id}/permissions', 'put')]:
                check('published ' + method + ' ' + path, method in spec.get('paths', {}).get(path, {}))
            for path, method in [('/api/packages', 'post'), ('/api/packages/{id}', 'put'),
                                 ('/api/packages/{id}/status', 'patch')]:
                operation = spec['paths'][path][method]
                check('retired metadata ' + path, operation.get('deprecated') is True
                      and '400' in operation['responses'] and '201' not in operation['responses']
                      and '200' not in operation['responses'])
        request('anonymous', 'GET', '/api/auth/me', expected=(401,))
        _, _, cors = request('public', 'OPTIONS', '/api/auth/login', expected=(200,),
                             headers={'Origin': args.frontend_origin,
                                      'Access-Control-Request-Method': 'POST',
                                      'Access-Control-Request-Headers': 'content-type'})
        check('frontend CORS credentials', cors.get('Access-Control-Allow-Origin') == args.frontend_origin
              and cors.get('Access-Control-Allow-Credentials') == 'true')
        actor_data = json.loads(Path(args.actors_file).read_text(encoding='utf-8-sig'))['actors']
        required_roles = {'Admin', 'CenterManager', 'Receptionist', 'Coach', 'Member'}
        check('five DEMO roles supplied', required_roles.issubset({a['role'] for a in actor_data}))
        for actor in actor_data:
            alias = actor['alias']
            contexts[alias] = urllib.request.build_opener(
                urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
            # Exactly one known password attempt: never guess or retry credentials.
            request(alias, 'POST', '/api/auth/login',
                    {'email': actor['email'], 'password': actor['password']}, expected=(200,))
            _, me, _ = request(alias, 'GET', '/api/auth/me', expected=(200,))
            check(alias + ':verified current identity', me.get('email', '').lower() == actor['email'].lower()
                  and me.get('role') == actor['role'] and not me.get('firstLogin', False))
            actor['me'] = me
            report['authenticatedActors'].append({'alias': alias, 'role': me['role'], 'userId': me['userId']})
            request(alias, 'GET', '/api/rooms', expected=(200,))
            request(alias, 'GET', '/api/notifications/me?page=0&size=3', expected=(200,))
            request(alias, 'GET', '/api/notifications/me/unread-count', expected=(200,))
            for path in ['/api/roles', '/api/permissions']:
                request(alias, 'GET', path, expected=(200,) if me['role'] == 'Admin' else (403,))
            if me['role'] == 'Member':
                _, profile, _ = request(alias, 'GET', '/api/members/me', expected=(200,))
                check(alias + ':profile whitelist', set(profile) ==
                      {'name', 'phone', 'address', 'dateOfBirth', 'gender', 'goal', 'healthNote'})
                for path in ['/api/training-results', '/api/training-plans', '/api/attendances',
                             '/api/members/me/invoices', '/api/normal-refunds']:
                    request(alias, 'GET', path, expected=(200,))
            elif me['role'] == 'Coach':
                for path in ['/api/training-results', '/api/training-plans', '/api/attendances']:
                    request(alias, 'GET', path, expected=(200,))
            else:
                authorities = set(me.get('authorities', []))
                request(alias, 'GET', '/api/training-results', expected=(200,) if
                        authorities.intersection({'MANAGE_CLASSES', 'MANAGE_USERS'}) else (403,))

        admin = next(a for a in actor_data if a['role'] == 'Admin')
        manager = next(a for a in actor_data if a['role'] == 'CenterManager')
        member = next(a for a in actor_data if a['role'] == 'Member')
        _, roles_before, _ = request(admin['alias'], 'GET', '/api/roles', expected=(200,))
        admin_role = next(r for r in roles_before if r['name'].lower() == 'admin')
        if args.negative_mutations:
            request(member['alias'], 'PUT', '/api/roles/' + str(admin_role['id']) + '/permissions',
                    {'permissionIds': []}, expected=(403,))
            _, roles_after, _ = request(admin['alias'], 'GET', '/api/roles', expected=(200,))
            def role_state(rows):
                return sorted((r['id'], sorted(p['id'] for p in r.get('permissions', []))) for r in rows)
            check('denied RBAC retains role permission sets', role_state(roles_before) == role_state(roles_after))
            _, rooms_before, _ = request(manager['alias'], 'GET', '/api/rooms', expected=(200,))
            request(manager['alias'], 'POST', '/api/rooms',
                    {'name': 'N' * 51, 'capacity': 4, 'activityType': 'BADMINTON'}, expected=(400,))
            available = [r for r in rooms_before if r.get('status') == 'Available']
            pair = next(((a, b) for a in available for b in available
                         if a['id'] != b['id'] and a.get('centerId') == b.get('centerId')), None)
            if pair:
                original, duplicate = pair
                body = {k: original.get(k) for k in
                        ['roomCode', 'name', 'location', 'capacity', 'status', 'facilityType', 'activityType']}
                body['name'] = duplicate['name']
                _, conflict, _ = request(manager['alias'], 'PUT', '/api/rooms/' + str(original['id']),
                                         body, expected=(409,))
                check('duplicate room error body', conflict.get('status') == 409
                      and conflict.get('error') == 'Conflict')
            else:
                checks.append({'case': 'duplicate room fixture', 'result': 'NOT_RUN',
                               'reason': 'No two Available rooms in the same permitted center'})
            _, rooms_after, _ = request(manager['alias'], 'GET', '/api/rooms', expected=(200,))
            stable = lambda rows: sorted(rows, key=lambda row: row['id'])
            check('invalid/duplicate room requests retain committed rooms', stable(rooms_before) == stable(rooms_after))
        _, final_health, _ = request('public', 'GET', '/api/health', expected=(200,))
        check('runtime stayed on expected release', final_health.get('buildCommit') == args.expected_commit)
        for actor in actor_data:
            request(actor['alias'], 'POST', '/api/auth/logout', expected=(204,))
            request(actor['alias'], 'GET', '/api/auth/me', expected=(401,))
        report['result'] = 'PASS_CHECKED_SCOPE'
    except Exception as error:
        report['result'] = 'FAIL'
        report['failure'] = {'type': type(error).__name__, 'case': str(error) if isinstance(error, AssertionError)
                             else 'Transport, setup or JSON error; credential/body details omitted'}
    finally:
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'result': report['result'], 'phase': args.phase, 'cases': len(checks),
                      'failed': [c['case'] for c in checks if c['result'] == 'FAIL'],
                      'authenticatedActors': len(report['authenticatedActors']), 'output': args.output}))
    return 0 if report['result'] == 'PASS_CHECKED_SCOPE' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='https://scms-app-p1gy.onrender.com')
    parser.add_argument('--frontend-origin', default='https://vibesports.pages.dev')
    parser.add_argument('--expected-commit', required=True)
    parser.add_argument('--expected-db-fingerprint')
    parser.add_argument('--actors-file', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--phase', choices=['baseline', 'post-deploy'], default='baseline')
    parser.add_argument('--negative-mutations', action='store_true')
    sys.exit(run(parser.parse_args()))
