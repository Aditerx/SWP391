import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';

const origin = 'http://127.0.0.1:5173';
const port = 9333;
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'scms-cash-demo-'));
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`,
  '--no-first-run', '--no-default-browser-check', '--window-size=1440,1050', 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
let socket;
let sequence = 0;
const pending = new Map();
const report = [];
const runtimeErrors = [];
let resumeContext = {};
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const id = ++sequence; pending.set(id, { resolve, reject });
  socket.send(JSON.stringify({ id, method, params }));
});
const evaluate = async (expression) => {
  const reply = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (reply.exceptionDetails) throw new Error(reply.exceptionDetails.exception?.description || reply.exceptionDetails.text);
  return reply.result.value;
};
const wait = async (expression, description, timeout = 60000) => {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    if (await evaluate(expression)) return;
    await delay(300);
  }
  const alerts = await evaluate("[...document.querySelectorAll('[role=alert]')].map(e=>e.textContent).join(' | ')");
  throw new Error(`Timeout: ${description}. ${alerts}`);
};
const navigate = async route => {
  await send('Page.navigate', { url: origin + route });
  await wait("document.readyState === 'complete' && !!document.querySelector('#root')", route);
};
const set = async (selector, value) => evaluate(`(() => { const el=document.querySelector(${JSON.stringify(selector)}); if(!el) throw new Error('Missing input'); const p=el.tagName==='SELECT'?HTMLSelectElement.prototype:el.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(p,'value').set.call(el,${JSON.stringify(String(value))}); el.dispatchEvent(new Event('input',{bubbles:true})); el.dispatchEvent(new Event('change',{bubbles:true})); })()`);
const click = async text => evaluate(`(() => { const el=[...document.querySelectorAll('button')].find(e=>e.textContent.trim()===${JSON.stringify(text)}); if(!el || el.disabled) throw new Error('Missing or disabled button: '+${JSON.stringify(text)}); el.click(); })()`);
const request = (route, data) => evaluate(`(async () => { const r=await fetch(${JSON.stringify(route)}, {method:${JSON.stringify(data === undefined ? 'GET' : 'POST')},credentials:'include',headers:{'Content-Type':'application/json'},${data === undefined ? '' : `body:JSON.stringify(${JSON.stringify(data)}),`} }); const value=r.status===204?null:await r.json(); return {status:r.status,value}; })()`);
const login = async (email, route) => {
  await request('/api/auth/logout', {});
  const response = await request('/api/auth/login', { email, password: 'DemoCash2026' });
  assert.equal(response.status, 200, 'Real login');
  await navigate(route);
};
const check = description => { if (!report.includes(description)) report.push(description); console.log('PASS:', description); };
const screenshot = async name => {
  const shot = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(`audit/${name}.png`, Buffer.from(shot.data, 'base64'));
};

try {
  let pages;
  for (let n = 0; n < 100; n++) {
    try { pages = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json()); if (pages.length) break; } catch {}
    await delay(200);
  }
  assert.ok(pages?.length, 'Chrome headless started');
  socket = new WebSocket(pages.find(p => p.type === 'page').webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  socket.onmessage = event => {
    const message = JSON.parse(event.data);
    if (message.id && pending.has(message.id)) {
      const promise = pending.get(message.id); pending.delete(message.id);
      message.error ? promise.reject(new Error(message.error.message)) : promise.resolve(message.result);
    }
    if (message.method === 'Runtime.exceptionThrown') runtimeErrors.push(message.params.exceptionDetails.text);
  };
  await send('Runtime.enable'); await send('Page.enable');
  let email, classId, me;
  if (process.argv.includes('--resume')) {
    const previous = JSON.parse(fs.readFileSync('audit/browser-cash-result.json', 'utf8'));
    report.push(...previous.checks);
    email = previous.memberEmail;
    classId = previous.classId;
    resumeContext = { memberEmail: email, classId };
    assert.ok(email && classId, 'Resume context');
    await navigate('/login');
  } else {
  await navigate('/register');
  await wait("!!document.querySelector('input[placeholder*=\"Họ\"]')", 'Registration form');
  email = `demo.browser.${Date.now()}@example.test`;
  resumeContext.memberEmail = email;
  await set('input[placeholder*="Họ"]', 'DEMO Browser Cash');
  await set('input[placeholder*="điện thoại"]', '0900000000');
  await set('input[type="email"]', email);
  const passwords = await evaluate("[...document.querySelectorAll('input[type=password]')].map(e=>e.placeholder)");
  for (const placeholder of passwords) await set(`input[placeholder=${JSON.stringify(placeholder)}]`, 'DemoCash2026');
  await evaluate("[...document.querySelectorAll('span')].find(e=>e.textContent==='Tôi không phải là người máy').parentElement.click()");
  await delay(450);
  await evaluate("document.querySelector('form').requestSubmit()");
  await wait("!!document.querySelector('#otp-0')", 'OTP screen');
  for (let n = 0; n < 6; n++) await set(`#otp-${n}`, String(n + 1));
  await click('Xác thực & Hoàn tất');
  await wait("document.body.innerText.includes('Đăng ký hội viên thành công')", 'Registration persisted');
  check('Real registration UI accepts backend demo OTP 123456');
  await login(email, '/member/my-membership');
  await wait("document.body.innerText.includes('DEMO CASH 30 ngày')", 'Membership packages');
  await evaluate("[...document.querySelectorAll('h3')].find(e=>e.textContent==='DEMO CASH 30 ngày').parentElement.querySelector('button').click()");
  await wait("document.body.innerText.includes('Yêu cầu SCMS-') || document.body.innerText.includes('Mang mã đến lễ tân')", 'Cash purchase order');
  let own = (await request('/api/members/me/invoices')).value;
  assert.equal(own.length, 1); const packageInvoice = own[0];
  assert.equal(packageInvoice.paymentStatus, 'Pending'); assert.equal(packageInvoice.amount, 300000);
  await send('Page.reload');
  await wait("document.body.innerText.includes('Chờ thanh toán')", 'Pending after F5');
  check('Member UI creates one Pending Cash invoice and retains it after F5');
  const settle = async (id, code) => {
    await login('demo.cash.reception.1010@example.test', '/receptionist/payments');
    await wait("!!document.querySelector('input[aria-label=\"Tìm hóa đơn\"]')", 'Cashier screen');
    await set('input[aria-label="Tìm hóa đơn"]', email);
    await wait(`!![...document.querySelectorAll('tr')].find(e=>e.textContent.includes(${JSON.stringify(email)}) && e.textContent.includes(${JSON.stringify(code)}) && !!e.querySelector('button:not(:disabled)'))`, 'Correct payable demo invoice');
    await evaluate(`(() => {const row=[...document.querySelectorAll('tr')].find(e=>e.textContent.includes(${JSON.stringify(email)}) && e.textContent.includes(${JSON.stringify(code)})); if(!row)throw new Error('Demo invoice not found'); row.querySelector('button').click();})()`);
    await wait("!!document.querySelector('[role=dialog]')", 'Cash confirmation dialog');
    await click('Đã nhận đủ tiền · Xác nhận');
    await wait("document.body.innerText.includes('Đã thu Cash.')", 'Cash settled');
  };
  await settle(packageInvoice.invoiceId, packageInvoice.invoiceCode);
  await screenshot('demo-cash-receptionist');
  await login(email, '/member/catalog');
  await wait("document.body.innerText.includes('DEMO CASH Có phí')", 'Class catalog');
  const selectClass = async fragment => evaluate(`(() => { const el=[...document.querySelectorAll('h3')].find(e=>e.textContent.includes(${JSON.stringify(fragment)})); el.parentElement.querySelector('button').click(); })()`);
  await selectClass('DEMO CASH Có phí');
  await wait("document.body.innerText.includes('Đã giữ chỗ.')", 'Paid class enrollment');
  own = (await request('/api/members/me/invoices')).value;
  const tuition = own.find(i => i.classId);
  assert.equal(tuition.amount, 150000); assert.equal(tuition.paymentStatus, 'Pending');
  classId = tuition.classId;
  resumeContext.classId = classId;
  check('Class UI uses DB tuition and reserves a Pending place');
  await settle(tuition.invoiceId, tuition.invoiceCode);
  await login(email, '/member/catalog');
  await wait("document.body.innerText.includes('Đã ghi danh')", 'Paid enrollment activated');
  await selectClass('DEMO CASH Miễn phí');
  await wait("document.body.innerText.includes('Đã ghi danh lớp miễn phí.')", 'Free enrollment');
  check('Cash payment activates paid enrollment; free enrollment has no invoice');
  me = (await request('/api/auth/me')).value;
  await login('demo.cash.coach.1010@example.test', '/coach/attendance');
  await wait("[...document.querySelectorAll('select option')].some(e=>e.textContent.includes('DEMO CASH Có phí'))", 'Coach sessions');
  const sessionId = await evaluate("[...document.querySelectorAll('select[aria-label=\"Chọn buổi học\"] option')].find(e=>e.textContent.includes('DEMO CASH Có phí')).value");
  await set('select[aria-label="Chọn buổi học"]', sessionId);
  await wait("document.body.innerText.includes('DEMO Browser Cash')", 'Actual roster');
  await evaluate(`(() => { const form=[...document.querySelectorAll('form')].find(f=>f.textContent.includes(${JSON.stringify(email)})); form.querySelector('button').click(); })()`);
  await wait(`(async()=>{const r=await fetch('/api/training-results?sessionId=${sessionId}',{credentials:'include'}); if(!r.ok)return false;return (await r.json()).some(t=>t.memberId===${me.memberId} && t.attendanceStatus==='Present');})()`, 'Attendance saved');
  check('Coach UI loads real roster and saves Present to backend');
  await screenshot('demo-cash-coach');
  }
  await login(email, '/member/catalog');
  const existingFeedback = await request(`/api/classes/${classId}/feedback/me`);
  if (existingFeedback.status !== 200) {
  await wait("document.body.innerText.includes('Điểm mentor')", 'Feedback controls');
  await wait("[...document.querySelectorAll('button')].some(e=>e.textContent==='Gửi feedback' && !e.disabled)", 'Feedback eligibility read completed');
  await set('textarea[aria-label="Nội dung feedback"]', 'Browser Cash demo feedback');
  await click('Gửi feedback');
  }
  await wait("document.body.innerText.includes('Đã gửi feedback')", 'Feedback saved');
  await send('Page.reload');
  await wait("document.body.innerText.includes('Đã gửi feedback')", 'Feedback persisted after F5');
  await screenshot('demo-cash-member');
  assert.equal((await request(`/api/classes/${classId}/feedback/me`)).status, 200);
  check('Member feedback UI persists after F5');
  await login('demo.cash.manager.1010@example.test', '/manager/classes');
  await wait("document.body.innerText.includes('Feedback lớp và mentor')", 'Manager feedback');
  await wait(`!!document.querySelector('select[aria-label="Lớp xem feedback"] option[value="${classId}"]')`, 'Manager class options loaded');
  await set('select[aria-label="Lớp xem feedback"]', classId);
  await wait("document.body.innerText.includes('DEMO Browser Cash')", 'Manager review');
  check('Manager UI reads real class feedback');
  await login('demo.cash.coach.1010@example.test', '/coach/attendance');
  await wait("document.body.innerText.includes('1 đánh giá')", 'Anonymous coach feedback');
  const coachFeedback = (await request(`/api/coaches/me/feedback?classId=${classId}`)).value;
  assert.ok(!('memberId' in coachFeedback.feedback[0]));
  assert.ok(!('memberName' in coachFeedback.feedback[0]));
  check('Coach feedback response omits member identity');
  assert.deepEqual(runtimeErrors, [], 'No uncaught browser exceptions');
  fs.writeFileSync('audit/browser-cash-result.json', JSON.stringify({ passed: true, checks: report, memberEmail: email, classId }, null, 2));
} catch (e) {
  fs.writeFileSync('audit/browser-cash-result.json', JSON.stringify({ passed: false, checks: report, error: e.message, runtimeErrors, ...resumeContext }, null, 2));
  console.error(e.message); process.exitCode = 1;
} finally {
  socket?.close(); chrome.kill();
}
