"""Summarize a successful isolated demo run; publish only sanitized HTTP trace and test summaries."""
from pathlib import Path
import argparse, hashlib, json, re, subprocess, xml.etree.ElementTree as ET

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('run_directory')
p.add_argument('--output', required=True)
a = p.parse_args()
run, out = Path(a.run_directory).resolve(), Path(a.output).resolve()
out.mkdir(parents=True, exist_ok=True)
verification = json.loads((run/'verification.json').read_text(encoding='utf-8-sig'))
assert verification['failures'] == verification['errors'] == 0
assert verification['postgresHttpFailures'] == verification['postgresHttpErrors'] == 0
suites = []
for file in sorted((run/'clean-xml').glob('TEST-*.xml')):
    root = ET.parse(file).getroot()
    suites.append({'name': root.get('name'), **{k: int(root.get(k, 0)) for k in ('tests','failures','errors','skipped')},
                   'cases': [{'name': c.get('name'), 'skipped': c.find('skipped') is not None} for c in root.findall('testcase')]})
pg = ET.parse(run/'postgres-workflow.xml').getroot()
assert int(pg.get('failures')) == int(pg.get('errors')) == int(pg.get('skipped')) == 0
verification.update(postgresHttpTests=int(pg.get('tests')), suites=suites,
                    postgresCases=[c.get('name') for c in pg.findall('testcase')],
                    scope='P1.0-P1.4 and backend P2; P1.5/T/D01/D09/FE excluded',
                    limits=['Local HTTP with real login and committed PostgreSQL assertions',
                            'Signed local VNPay protocol; no live gateway or real money',
                            'No SMTP delivery, FE browser, SQL Server runtime or Render deploy certification'])
trace = (run/'postgres-http/http-workflow.txt').read_text(encoding='utf-8')
trace = re.sub(r'vnp_SecureHash=[^&\s"]*', 'vnp_SecureHash=[redacted]', trace)
trace = '\n'.join(line.rstrip() for line in trace.splitlines())+'\n'
(out/'postgres-http.txt').write_text(trace, encoding='utf-8',newline='\n')
summary = []
for name in ('clean-test.log','postgres-http.log','postgres-prepare.log'):
    data = (run/name).read_bytes()
    # Windows PowerShell redirection can produce UTF-16 logs.
    text = data.decode('utf-16') if data.startswith(b'\xff\xfe') else data.decode('utf-8', errors='replace')
    summary.append(name+' SHA256 '+hashlib.sha256(data).hexdigest())
    summary.extend(line for line in text.splitlines() if re.match(r'^\[(?:INFO|WARNING)\] (?:Tests run:|BUILD SUCCESS|Total time:|Finished at:)', line))
(out/'test-summary.txt').write_text('\n'.join(summary)+'\n',encoding='utf-8',newline='\n')
source = verification['sourceCommit']
tree = subprocess.check_output(['git','ls-tree','-r',source,'--','src','database','schema_postgres.sql','pom.xml','scripts','audit'],text=True)
(out/'source-tree.txt').write_text(tree,encoding='utf-8',newline='\n')
verification['evidenceHashes'] = {name: hashlib.sha256((out/name).read_bytes()).hexdigest()
                                  for name in ('postgres-http.txt','test-summary.txt','source-tree.txt')}
exchanges = run/'postgres-http/http-exchanges.json'
if exchanges.exists():
    safe_exchanges = re.sub(r'vnp_SecureHash=[^&"\\]*', 'vnp_SecureHash=[redacted]', exchanges.read_text(encoding='utf-8'))
    json.loads(safe_exchanges)
    (out/'http-exchanges.json').write_text(safe_exchanges+'\n',encoding='utf-8',newline='\n')
    verification['evidenceHashes']['http-exchanges.json'] = hashlib.sha256((out/'http-exchanges.json').read_bytes()).hexdigest()
(out/'verification.json').write_text(json.dumps(verification,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
payload = json.dumps({'v': verification, 'trace': trace},ensure_ascii=False).replace('<','\\u003c')
html = '''<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SCMS · Demo hai phase BE</title><style>
*{box-sizing:border-box}body{margin:0;background:#101824;color:#e4edf7;font:16px/1.55 system-ui,sans-serif}main{max-width:1120px;margin:auto;padding:42px 24px}small,.muted{color:#a5bbd2}h1{font-size:38px;line-height:1.15}h2{font-size:23px;margin-top:32px}.badge{display:inline-block;background:#173f39;color:#99f1c5;padding:6px 12px;border-radius:8px}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px}.card{background:#192638;border:1px solid #30425a;padding:18px;border-radius:12px}.number{font-size:30px;font-weight:700;color:#93d4fa}input{width:100%;padding:13px;margin:14px 0;background:#192638;border:1px solid #6c91b7;border-radius:8px;color:white;font:inherit}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#0b121c;padding:18px;border:1px solid #30425a;border-radius:10px;font:12px/1.65 Consolas,monospace;max-height:620px;overflow:auto}a{color:#9bd7ff}li{margin:8px 0}code{color:#c6d9ed}button{padding:8px 14px;margin:5px 4px 5px 0;border:1px solid #456682;border-radius:7px;background:#192638;color:#d9ecff;cursor:pointer}button:hover{background:#29415a}</style>
<main><span class="badge">Bằng chứng demo BE local · PostgreSQL</span><h1>SCMS — hai phase từ mua quyền đến hoàn tất buổi học</h1>
<p class="muted">Bản ghi kiểm chứng từ commit <code id="sha"></code>. Trang này xem evidence đã ghi; không kết nối production và không phải giao diện FE tích hợp.</p>
<div class="cards" id="stats"></div><h2>Câu chuyện demo 15–20 phút</h2><ol>
<li>Member chọn môn/Coach → Coach nhận → Invoice Pending. Cash/Paid mới cấp 12 credit, giá giữ nguyên khi catalog đổi.</li>
<li>Return chưa ghi tiền; IPN ký hợp lệ ghi một lần. Tiền muộn hoặc không thể cấp quyền được giữ để đối soát.</li>
<li>Coach tạo ca có consent; Member hủy đúng12h. Notice dưới12h tách khỏi attendance; dời lịch giữ audit.</li>
<li>Holiday preview phát hiện tác động/stale; incident chờ khắc phục, union thời gian nghỉ và phục hồi credit gốc qua expiry.</li>
<li>QR chỉ trong giờ; replay bị chặn; correction sau giờ có lý do. Normal chỉ bù buổi thực sự không tổ chức vì TT.</li>
<li>Refund có chứng cứ/khác kỳ; sửa nhầm Cash bị chặn khi có history. Báo cáo phân biệt thu, hoàn, ròng và quyền theo người.</li></ol>
<h2>Request/response đã làm sạch</h2><p class="muted">Login/cookie, QR payload và chữ ký callback được loại khỏi trace. Các xác minh DB nằm trong PhaseWorkflowHttpTest.</p>
<div id="filters"></div><input id="search" placeholder="Lọc endpoint, mã lỗi hoặc trạng thái…" aria-label="Lọc trace"><small id="count"></small><pre id="trace"></pre>
<h2>Kết quả và cách chạy lại</h2><pre id="cases"></pre><p><code>.\\scripts\\run-phases-demo.ps1</code> — tạo committed checkout và DB local mới, chạy clean test + migration guards + HTTP PostgreSQL.</p>
<p><a href="verification.json">Manifest kết quả</a> · <a href="test-summary.txt">Test summary</a> · <a href="postgres-http.txt">Trace đầy đủ</a> · <a href="../README.md">Bàn giao/hướng dẫn</a></p>
<p class="muted">P1.5/gói T D01/D09/FE để lại theo Owner. Không chứng nhận gateway live, SMTP delivery, SQL Server runtime hoặc Render từ kết quả local.</p></main>
<script>const data=PAYLOAD;const v=data.v;document.querySelector('#sha').textContent=v.sourceCommit;
document.querySelector('#stats').innerHTML=[[v.tests-v.skipped,'tests pass'],[v.skipped,'opt-in tests bỏ qua'],[v.postgresHttpTests,'HTTP workflow PG pass'],[0,'failure / error']].map(x=>'<div class="card"><div class="number">'+x[0]+'</div>'+x[1]+'</div>').join('');
const lines=data.trace.trim().split('\\n'),search=document.querySelector('#search');function render(){const q=search.value.toLowerCase(),rows=lines.filter(x=>x.toLowerCase().includes(q));document.querySelector('#trace').textContent=rows.join('\\n');document.querySelector('#count').textContent=rows.length+' / '+lines.length+' thao tác';}search.oninput=render;
for(const [label,q] of [['Tất cả',''],['Mua quyền','advanced-registrations'],['Thanh toán','vnpay'],['Lịch','/sessions/'],['Sự cố','center-incidents'],['Holiday','holidays'],['QR','attendances'],['Hoàn/sửa Cash','payment-resolutions'],['Báo cáo','reports']]){const b=document.createElement('button');b.textContent=label;b.onclick=()=>{search.value=q;render();};document.querySelector('#filters').append(b);}
document.querySelector('#cases').textContent=v.postgresCases.map(x=>'PASS  '+x).join('\\n');render();</script></html>'''
(out/'demo.html').write_text(html.replace('PAYLOAD',payload),encoding='utf-8',newline='\n')
print(json.dumps({'sourceCommit':source,'tests':verification['tests'],'skipped':verification['skipped'],
                  'postgresHttpTests':int(pg.get('tests')),'output':str(out)}))
