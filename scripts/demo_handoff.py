"""Build readable lookup tables from the same sanitized rows exported to SYSTEM-DATA."""
from datetime import datetime, timedelta

PURPOSES = {
    'N01': ('Thu Cash rồi xác nhận đủ sĩ số', 'demo-02'),
    'N02': ('Cutoff đăng ký và xác nhận trễ', 'demo-04'),
    'N03': ('Chặn xác nhận khi thiếu sĩ số', 'demo-06'),
    'N04': ('Đã dời một lần; chặn dời lần hai', 'demo-04-07'),
    'N05': ('Lớp nguồn đang đề nghị chuyển', 'demo-08'),
    'N06': ('TT đã hủy; nghĩa vụ hoàn RF02–RF04', 'demo-09'),
    'N07': ('Gần đầy; chuẩn bị chỗ cuối/waitlist, ngoài luồng chính', 'normal-scenario-map'),
    'N08': ('Ongoing; chặn hủy theo luồng trước khai giảng', 'demo-07'),
    'N09': ('Closed; xem lịch sử/kết quả, không thay kiểm job đóng lớp', 'demo-10'),
    'N10': ('Sự cố TT/buổi bù; xem nguồn và lịch sử', 'demo-10'),
    'N11': ('Lớp đích N05, ban đầu còn một chỗ', 'demo-08'),
    'N12': ('TT đã hủy; xem các state hoàn RF05–RF07', 'demo-01'),
}
ACTORS = {'ADM','CM1','R1','BAD-A','PICK-A','PICK-B','M01','M02','M03','M04','M14','M15'}


def one(values, label):
    if len(values) != 1:
        raise ValueError('Expected one pristine fixture binding for '+label)
    return values[0]


def catalog(rows, design):
    """Resolve aliases by identity/relationships, never by an ID offset or row order."""
    roles = {r['role_id']:r['role_name'] for r in rows['roles']}
    users = {}
    for expected in design['users']:
        actual = one([u for u in rows['users'] if u['email'].casefold()==expected['email'].casefold()],expected['key'])
        if roles[actual['role_id']] != expected['role']:
            raise ValueError('Fixture role mismatch for '+expected['key'])
        users[expected['key']] = actual
    by_user_id = {u['user_id']:u for u in rows['users']}
    rooms = {r['room_id']:r for r in rows['rooms']}
    classes = {}
    for expected in design['classes']:
        if expected['type']=='Normal':
            classes[expected['key']] = one([c for c in rows['classes'] if c['class_type']=='Normal' and c['class_name']==expected['name']],expected['key'])
    if set(classes) != set(PURPOSES):
        raise ValueError('Expected the approved N01–N12 catalog')
    paid = {(i['member_id'],i.get('entitlement_class_id') or i['class_id']) for i in rows['invoices']
            if i['payment_status']=='Paid' and not i['reconciliation_required'] and not i['cash_confirmation_reversed']}
    class_rows = []
    for key,c in sorted(classes.items()):
        schedule = sorted([s for s in rows['sessions'] if s['class_id']==c['class_id'] and not s['center_invalidated']],key=lambda s:(s['session_date'],s['start_time'],s['session_id']))
        if not schedule:raise ValueError('Missing schedule for '+key)
        first=schedule[0];at=datetime.fromisoformat(first['session_date']+'T'+first['start_time'])
        count=sum(e['class_id']==c['class_id'] and e['status']=='Registered' and (e['member_id'],c['class_id']) in paid
                  and by_user_id[e['member_id']]['status']=='Active' and by_user_id[e['member_id']]['center_id']==c['center_id']
                  for e in rows['class_enrollments'])
        class_rows.append({'key':key,'classId':c['class_id'],'name':c['class_name'],'state':c['status'],
                           'paidRegistered':count,'minimum':c['min_capacity'],'maximum':c['max_capacity'],
                           'classCoach':by_user_id[c['coach_id']],'firstSessionCoach':by_user_id[first['coach_id'] or c['coach_id']],
                           'firstSessionId':first['session_id'],'firstSession':at,'registrationCutoff':at-timedelta(hours=48),
                           'reasonFrom':at-timedelta(hours=24),'firstRoom':rooms[first['room_id']]})
    reverse={c['class_id']:key for key,c in classes.items()}
    proposals=[]
    for p in sorted(rows['normal_proposals'],key=lambda p:p['id']):
        key=reverse[p['class_id']]
        event=one([e for e in design['normal_class_events'] if e['sports_class']==key and e['kind'] in ['TRANSFER_OFFER','POSTPONE']],key+' proposal')
        proposals.append({'key':event['key'],'id':p['id'],'source':key,'target':reverse.get(p['target_class_id']),
                          'kind':p['kind'],'state':p['status']})
    refunds=[]
    for expected in design['payment_resolutions']:
        if expected['type']!='Refund':continue
        inv_design=one([i for i in design['invoices'] if i['key']==expected['invoice']],expected['key']+' source')
        class_key=inv_design['sports_class'];member=users[inv_design['member']]
        invoice=one([i for i in rows['invoices'] if i['member_id']==member['user_id'] and i['class_id']==classes[class_key]['class_id']],expected['key']+' Invoice')
        resolution=one([r for r in rows['payment_resolutions'] if r['invoice_id']==invoice['invoice_id'] and r['resolution_type']=='Refund'],expected['key']+' resolution')
        refund=one([f for f in rows['normal_refunds'] if f['id']==resolution['id']],expected['key']+' Refund')
        refunds.append({'key':expected['key'],'refundId':refund['id'],'invoiceId':invoice['invoice_id'],
                        'memberKey':inv_design['member'],'member':member,'classKey':class_key,
                        'state':resolution['status'],'amount':resolution['amount']})
    return {'classes':class_rows,'actors':[{'key':key,'user':u,'role':roles[u['role_id']]} for key,u in users.items() if key in ACTORS],
            'proposals':proposals,'refunds':refunds}


def render(rows, design):
    index=catalog(rows,design)
    def cell(value):return str(value).replace('|','\\|').replace('\n','<br>')
    def line(values):return '| '+' | '.join(cell(v) for v in values)+' |'
    def person(u):return f"{u['full_name']} (userId={u['user_id']})"
    def stamp(at):return at.strftime('%Y-%m-%d %H:%M')
    out=['''<a id="normal-class-index"></a>
## Tra cứu N01–N12 — lớp và mục đích demo

Nxx là mã lớp Normal trong fixture, không phải classId hoặc thứ tự thao tác. Bảng này được sinh từ chính các dòng local bên dưới; đây là trạng thái **nguyên trạng trước demo**, không phải DB cloud sau smoke. Paid Registered/min/max nghĩa là số người Paid còn quyền / minimum / sức chứa. Các giới hạn này chưa chứng nhận lớp đủ mọi điều kiện khai giảng.

Nguồn: [classes](#table-classes), [enrollments](#table-class_enrollments), [invoices](#table-invoices). Case thao tác dùng mã DEMO-xx trong [SCRIPT](SCMS-SCRIPT-DEMO-BE.md#normal-scenario-map); Dxx vẫn là mã quyết định trong docs04.

| Mã | Tên lớp | classId | State | Paid Registered/min/max | Mục đích/case |
|---|---|---:|---|---|---|''']
    for c in index['classes']:
        purpose,anchor=PURPOSES[c['key']]
        out.append(line([c['key'],c['name'],c['classId'],c['state'],f"{c['paidRegistered']}/{c['minimum']}/{c['maximum']}",f'[{purpose}](SCMS-SCRIPT-DEMO-BE.md#{anchor})']))
    out+=['''
### Lịch và tài nguyên đối chiếu — toàn bộ giờ VN

Coach lớp lấy từ classes; Coach/sân buổi đầu lấy từ sessions, không suy rằng mọi buổi đều cùng tài nguyên. Lịch của lớp Cancelled vẫn được giữ để đối chiếu. Xem chi tiết [sessions](#table-sessions), [users](#table-users), [rooms](#table-rooms).

| Mã | Buổi đầu / sessionId | Cutoff đăng ký48h | Từ mốc cần reason24h | Coach lớp | Coach buổi đầu | Sân buổi đầu |
|---|---|---|---|---|---|---|''']
    for c in index['classes']:
        out.append(line([c['key'],f"{stamp(c['firstSession'])} / {c['firstSessionId']}",stamp(c['registrationCutoff']),stamp(c['reasonFrom']),person(c['classCoach']),person(c['firstSessionCoach']),f"{c['firstRoom']['room_code']} (roomId={c['firstRoom']['room_id']})"]))
    out+=['''
<a id="demo-actor-index"></a>
## Tài khoản dùng trong SCRIPT

Mxx/CM1/R1/ADM là mã đối chiếu fixture, không phải userId hoặc chuỗi gửi thay ID trong body. Email là định danh đăng nhập demo; mật khẩu truyền riêng. R1 ở bảng này là Receptionist, khác mã hạng mục R1/R2 của P2.2. Xem toàn bộ [users](#table-users) và [roles](#table-roles).

| Mã | Họ tên | userId | Role | Email đăng nhập demo |
|---|---|---:|---|---|''']
    for a in index['actors']:
        u=a['user'];out.append(line([a['key'],u['full_name'],u['user_id'],a['role'],u['email']]))
    out+=['''
<a id="demo-financial-index"></a>
## Proposal và Refund đối chiếu

EV/RF là mã fixture, không phải ID API. EV-N06/EV-N12 mô tả sự kiện TT hủy trong thiết kế; không suy ra có normal_proposals tương ứng. Chỉ các proposal thực có bên dưới được ánh xạ. Dùng GET hiện hành để lấy version/fingerprint/ID khi thao tác. Nguồn: [normal_proposals](#table-normal_proposals), [normal_refunds](#table-normal_refunds), [payment_resolutions](#table-payment_resolutions), [invoices](#table-invoices).

| Mã sự kiện | proposalId | Lớp nguồn → đích | Kind | State |
|---|---:|---|---|---|''']
    for p in index['proposals']:out.append(line([p['key'],p['id'],p['source']+' → '+(p['target'] or 'không có lớp đích'),p['kind'],p['state']]))
    out+=['','| Mã hoàn | refundId | Invoice nguồn | Member / memberId | Lớp nguồn | State | Amount VND |','|---|---:|---:|---|---|---|---:|']
    for f in index['refunds']:
        amount=f"{f['amount']:,.0f}".replace(',','.')
        out.append(line([f['key'],f['refundId'],f['invoiceId'],f"{f['memberKey']} / {f['member']['user_id']}",f['classKey'],f['state'],amount]))
    out.append('\nBảng tra cứu không chứa accountNumber hoặc proof; chỉ đọc các trường đó qua API đúng quyền. ID ở đây thuộc fixture local cùng anchor; kiểm lại runtime sau mọi lần dựng lại.\n')
    return '\n'.join(out)
