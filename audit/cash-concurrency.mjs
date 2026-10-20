import fs from 'node:fs';
import assert from 'node:assert/strict';
const base = 'http://127.0.0.1:8080';
const password = 'DemoCash2026';
const report = [];
async function call(route, cookie, data) {
  const response = await fetch(base + route, { method: data === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(cookie ? { Cookie: cookie } : {}) },
    ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
  const value = response.status === 204 ? null : await response.json();
  return { status: response.status, value, cookie: response.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ') };
}
async function login(email) {
  const result = await call('/api/auth/login', null, { email, password });
  assert.equal(result.status, 200); assert.ok(result.cookie); return result.cookie;
}
const check = message => { report.push(message); console.log('PASS:', message); };
try {
  const browser = JSON.parse(fs.readFileSync('audit/browser-cash-result.json', 'utf8'));
  assert.equal(browser.passed, true, 'Run browser Cash acceptance first');
  const first = await login(browser.memberEmail);
  const manager = await login('demo.cash.manager.1010@example.test');
  const cashier = await login('demo.cash.reception.1010@example.test');
  const pkgs = (await call('/api/packages', first)).value;
  const pkg = pkgs.find(p => p.name === 'DEMO CASH 30 ngày');
  const order = await call('/api/members/me/subscriptions', first, { packageId: pkg.id, paymentMethod: 'Cash' });
  assert.equal(order.status, 201);
  const confirm = await Promise.all([call(`/api/invoices/${order.value.invoiceId}/confirm-cash`, cashier, {}), call(`/api/invoices/${order.value.invoiceId}/confirm-cash`, cashier, {})]);
  assert.deepEqual(confirm.map(r => r.status).sort(), [200, 409]);
  assert.equal((await call(`/api/members/me/invoices/${order.value.invoiceId}`, first)).value.paymentStatus, 'Paid');
  check('Two concurrent Cash confirmations produce one Paid result and one 409');
  const secondEmail = `demo.concurrent.${Date.now()}@example.test`;
  assert.equal((await call('/api/auth/register', null, { fullName: 'DEMO Concurrency Member', email: secondEmail, password, phone: '0900000000' })).status, 200);
  assert.equal((await call('/api/auth/verify-registration', null, { email: secondEmail, otp: '123456' })).status, 204);
  const second = await login(secondEmail);
  const secondOrder = await call('/api/members/me/subscriptions', second, { packageId: pkg.id, paymentMethod: 'Cash' });
  assert.equal(secondOrder.status, 201);
  assert.equal((await call(`/api/invoices/${secondOrder.value.invoiceId}/confirm-cash`, cashier, {})).status, 200);
  const coach = (await call('/api/staff', manager)).value.find(c => c.email === 'demo.cash.coach.1010@example.test');
  const referenceClass = (await call(`/api/classes/${browser.classId}`, manager)).value;
  assert.ok(coach && referenceClass.subjectId, 'Demo coach and subject are available');
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());
  const cls = await call('/api/classes', manager, { name: `DEMO CASH Last Seat ${Date.now()}`, subjectId: referenceClass.subjectId, coachId: coach.id, maxCapacity: 1, tuitionFee: 100000, startDate: today, endDate: today, status: 'Open' });
  assert.equal(cls.status, 201);
  const enrollments = await Promise.all([call(`/api/classes/${cls.value.id}/enroll`, first, { paymentMethod: 'Cash' }), call(`/api/classes/${cls.value.id}/enroll`, second, { paymentMethod: 'Cash' })]);
  assert.deepEqual(enrollments.map(r => r.status).sort(), [201, 400]);
  const roster = (await call(`/api/classes/${cls.value.id}/enrollments`, manager)).value;
  assert.equal(roster.filter(e => e.status === 'Pending').length, 1);
  check('Two members racing for the last paid seat produce only one Pending enrollment');
  const winning = enrollments.find(r => r.status === 201).value;
  assert.equal((await call(`/api/invoices/${winning.invoiceId}/confirm-cash`, cashier, {})).status, 200);
  const paidRoster = (await call(`/api/classes/${cls.value.id}/enrollments`, manager)).value;
  assert.equal(paidRoster.filter(e => e.status === 'Registered').length, 1);
  check('The reserved last seat becomes Registered after Cash confirmation');
  // Use the losing member to test unpaid subscription cancellation and privacy boundaries.
  const cancelled = await call('/api/members/me/subscriptions', second, { packageId: pkg.id, paymentMethod: 'Cash' });
  assert.equal(cancelled.status, 201);
  assert.equal((await call(`/api/invoices/${cancelled.value.invoiceId}/cancel`, second, {})).status, 200);
  assert.equal((await call(`/api/invoices/${cancelled.value.invoiceId}/confirm-cash`, cashier, {})).status, 409);
  const memberId = (await call('/api/auth/me', second)).value.memberId;
  const subscriptions = (await call(`/api/members/${memberId}/subscriptions`, second)).value;
  assert.equal(subscriptions.find(s => s.subscriptionId === cancelled.value.subscriptionId).status, 'Cancelled');
  assert.equal((await call(`/api/members/me/invoices/${order.value.invoiceId}`, second)).status, 404);
  check('Pending cancellation synchronizes subscription/invoice; foreign invoice read returns 404');
  fs.writeFileSync('audit/cash-concurrency-result.json', JSON.stringify({ passed: true, checks: report }, null, 2));
} catch (e) {
  fs.writeFileSync('audit/cash-concurrency-result.json', JSON.stringify({ passed: false, checks: report, error: e.message }, null, 2));
  console.error(e.message); process.exitCode = 1;
}
