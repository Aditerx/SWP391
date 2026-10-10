import fs from 'node:fs';
const base='http://127.0.0.1:8080';
const login=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'demo.cash.reception.1010@example.test',password:'DemoCash2026'})});
if(!login.ok)throw new Error('Demo receptionist login failed');
const cookie=login.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ');
const memberResponse=await fetch(base+'/api/members',{headers:{Cookie:cookie}});
const members=await memberResponse.json();
if(!Array.isArray(members)) { console.log('Member lookup:',memberResponse.status,members.message); const me=await fetch(base+'/api/auth/me',{headers:{Cookie:cookie}}).then(r=>r.json()); console.log('Authorities:',me.authorities); throw new Error('Member lookup failed'); }
const member=members.filter(m=>m.name==='DEMO Browser Cash').sort((a,b)=>b.id-a.id)[0];
const invoices=await fetch(base+`/api/invoices?memberId=${member.id}`,{headers:{Cookie:cookie}}).then(r=>r.json());
const previous=JSON.parse(fs.readFileSync('audit/browser-cash-result.json','utf8'));
previous.memberEmail=member.email; previous.classId=invoices.find(i=>i.classId).classId;
fs.writeFileSync('audit/browser-cash-result.json',JSON.stringify(previous,null,2));
console.log('Recovered only the DEMO browser member and class for continuation.');
