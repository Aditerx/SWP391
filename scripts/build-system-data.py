"""Render the approved demo DATA DESIGN for FE. Never connects to or seeds a database."""
from pathlib import Path
from datetime import date, datetime, time, timedelta
import argparse
import ast
import hashlib
import json
import re
import uuid

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true')
parser.add_argument('--anchor', default='2026-11-10', help='Vietnam business date for the reproducible demo design')
parser.add_argument('--manifest', help='Write the same validated design as JSON for the SQL fixture compiler')
parser.add_argument('--no-render',action='store_true',help='Compile source rows without overwriting the reviewed SYSTEM-DATA export')
parser.add_argument('--from-local-pg',help='Export actual fixture rows from an explicitly disposable loopback database')
args = parser.parse_args()
if not args.no_render and not args.from_local_pg:
    parser.error("Use --from-local-pg scms_schema_demo_phase1_<new> to render/check verified fixture rows; --no-render compiles the design only")
ANCHOR = date.fromisoformat(args.anchor)
tables = {}
notes = {}


def sql_values(path, marker, end=';'):
    text = (ROOT / path).read_text(encoding='utf-8')
    block = text.split(marker, 1)[1].split(end, 1)[0]
    return [ast.literal_eval(re.sub(r'\bTRUE\b', 'True', re.sub(r'\bFALSE\b', 'False', line.strip().rstrip(','))))
            for line in block.splitlines() if line.strip().startswith('(')]


def add(table, **row):
    tables.setdefault(table, []).append(row)


def day(offset):
    return (ANCHOR + timedelta(days=offset)).isoformat()


def stamp(offset, hour='08:00'):
    return day(offset) + 'T' + hour


reference = 'database/bootstrap/scms_demo_reference.sql'
accounts = sql_values(reference, 'INSERT INTO _scms_accounts VALUES')
subjects = sql_values(reference, 'INSERT INTO _scms_subjects VALUES')
prefix = {'tennis': 'TEN', 'swimming': 'SWIM', 'pickleball': 'PICK', 'archery': 'ARC',
          'basketball': 'BR', 'football': 'FOOT', 'volleyball': 'BC', 'badminton': 'BAD', 'tabletennis': 'TT'}
subject_keys = {row[0]: prefix[row[7]] for row in subjects}
subject_data = {subject_keys[row[0]]: row for row in subjects}
member_index = coach_index = receptionist_index = manager_index = 0
for ordinal, role, name, email, phone, address, gender, birth in accounts:
    if role == 'Member':
        member_index += 1
        key = f'M{member_index:02}'
    elif role == 'Coach':
        match = re.fullmatch(r'coach\.([a-z]+)([12])@gmail\.com', email)
        key = prefix[match[1]] + ('-A' if match[2] == '1' else '-B')
    elif role == 'Receptionist':
        receptionist_index += 1
        key = 'R' + str(receptionist_index)
    elif role == 'CenterManager':
        manager_index += 1
        key = 'CM' + str(manager_index)
    else:
        key = 'ADM'
    add('users', key=key, pdf_ordinal=ordinal, role=role, name=name, email=email, phone=phone,
        address=address, gender=gender, date_of_birth=birth, status='Active',
        center=None if role == 'Admin' else 'C01', first_login=False)
for number in range(11, 21):
    add('users', key=f'M{number:02}', pdf_ordinal=None, role='Member', name=f'Member Demo{number}',
        email=f'member{number}@scms.demo.invalid', phone=None, address=None, gender=None,
        date_of_birth=None, status='Active', center='C01', first_login=False)
for key, role, state, email in [('MI', 'Member', 'Inactive', 'member.inactive'),
                                ('ML', 'Member', 'Locked', 'member.locked'),
                                ('MP', 'Member', 'Pending', 'member.pending'),
                                ('CI', 'Coach', 'Inactive', 'coach.inactive')]:
    add('users', key=key, pdf_ordinal=None, role=role, name='Demo ' + state + ' ' + role,
        email=email + '@scms.demo.invalid', phone=None, address=None, gender=None,
        date_of_birth=None, status=state, center='C01', first_login=False)

add('centers', key='C01', name='Sports Center 1', weekly_closure='Không mặc định nghỉ T7/CN')
roles = sql_values('database/bootstrap/scms_structure_postgres.sql', 'INSERT INTO roles (role_name, description) VALUES')
permissions = sql_values('database/bootstrap/scms_structure_postgres.sql', 'INSERT INTO permissions (permission_name, description) VALUES')
assignments = sql_values('database/bootstrap/scms_structure_postgres.sql', 'FROM (VALUES', ') AS defaults')
for name, description in roles:
    add('roles', key=name, description=description)
for name, description in permissions:
    add('permissions', key=name, description=description)
for role, permission in assignments:
    add('role_permissions', role=role, permission=permission)
for user in tables['users']:
    role = user['role']
    if role == 'Member':
        add('members', user=user['key'], join_date=day(-90), registered_by=None, goal=None, health_note=None)
    elif role == 'Coach':
        add('coaches', user=user['key'], specialization='Cầu lông' if user['key'] == 'CI' else subject_data[user['key'].split('-')[0]][1],
            bio='Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt')
    elif role == 'Receptionist':
        add('receptionists', user=user['key'])
    elif role == 'CenterManager':
        add('center_managers', user=user['key'])
for _, name, slug, normal, advanced, outdoor, _, coach_code in subjects:
    key = prefix[coach_code]
    add('subjects', key=key, name=name, slug=slug, normal_price=normal, advanced_price=advanced,
        outdoor=outdoor, status='Active')
    add('specializations', key='SP-' + key, subject=key, name=name, status='Active')
    for suffix in ['A', 'B']:
        add('coach_specializations', coach=key + '-' + suffix, specialization='SP-' + key)
add('coach_specializations', coach='CI', specialization='SP-BAD')
rooms = sql_values('database/migrations/13_room_catalog_postgres.sql', 'INSERT INTO _room_catalog VALUES')
activities = {'TEN': 'TENNIS', 'SWIM': 'SWIMMING', 'PICK': 'PICKLEBALL', 'ARC': 'ARCHERY',
              'BR': 'BASKETBALL', 'FOOT': 'FOOTBALL', 'BC': 'VOLLEYBALL', 'BAD': 'BADMINTON', 'TT': 'TABLE_TENNIS'}
for code, name, location, kind, capacity in rooms:
    add('rooms', key=code, name=name, location=location, facility_type=kind, capacity=capacity,
        center='C01', status='Available', activity_type=activities[code.split('-')[1]])
for user in tables['users']:
    if user['role'] == 'Coach' and user['status'] == 'Active':
        add('coach_work_schedules', coach=user['key'], accepting_new=True)
        for weekday in ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']:
            for slot in range(1, 8):
                add('coach_work_slots', coach=user['key'], slot_key=f'{weekday}:{slot}')
add('coach_days_off', coach='PICK-B', date=day(60))
add('holidays', key='H01', date=day(50), name='DEMO trung tâm đóng cửa', tag='CENTER_EVENT', is_active=True)

slots = {1: ('06:00', '07:30'), 2: ('07:45', '09:15'), 3: ('09:30', '11:00'),
         4: ('13:00', '14:30'), 5: ('14:45', '16:15'), 6: ('16:30', '18:00'), 7: ('18:15', '19:45')}
for index, row in enumerate(subjects, 1):
    subject = subject_keys[index]
    add('classes', key=f'A{index:02}', name=row[1] + ' Advanced', subject=subject,
        type='Advanced', coach=None, room=None, max_capacity=1, min_capacity=None,
        price=row[4], status='Open', sessions=0, lifecycle='Template, không phải quyền đã mua')

# key, subject, state, capacity, min, paid members, first offset, weekdays(ISO), slot, completed count
normal = [
 ('N01','BAD','Open',4,3,['M01'],7,[1,3,5],3,0),
 ('N02','TEN','Open',4,3,['M04','M05','M06'],1,[1,3,5],1,0),
 ('N03','SWIM','Open',10,7,[f'M{i:02}' for i in range(7,13)],1,[3,4,6],1,0),
 ('N04','ARC','Open',5,4,['M01','M02','M03'],7,[1,3,5],2,0),
 ('N05','PICK','Open',4,3,['M01','M02','M03'],7,[2,4,6],3,0),
 ('N06','BR','Cancelled',8,6,['M04','M05','M06'],8,[1,3,5],4,0),
 ('N07','FOOT','Open',15,10,[f'M{i:02}' for i in range(7,21)],9,[2,4,6],2,0),
 ('N08','BC','Ongoing',8,6,[f'M{i:02}' for i in range(1,7)],-11,[2,4,6],4,4),
 ('N09','TT','Closed',2,2,['M01','M02'],-50,[1,3,5],5,12),
 ('N10','BAD','Ongoing',4,3,['M01','M02','M03'],-20,[2,4,6],5,8),
 ('N11','PICK','Open',4,3,['M11','M12','M13'],7,[1,3,5],6,0),
 ('N12','BAD','Cancelled',4,3,['M07','M08','M09'],-20,[2,4,6],6,0),
]
paid_members = {}


def dates_from(offset, weekdays, count=12):
    cursor = ANCHOR + timedelta(days=offset)
    result = []
    while len(result) < count:
        if cursor.isoweekday() in weekdays and cursor.isoformat() != day(50):
            result.append(cursor)
        cursor += timedelta(days=1)
    return result


for key, subject, state, capacity, minimum, members, offset, weekdays, slot, completed in normal:
    paid_members[key] = members
    coach = subject + ('-B' if key == 'N11' else '-A')
    room = 'C01-' + subject + ('-02' if key == 'N11' else '-01')
    dates = dates_from(offset, weekdays)
    if state == 'Ongoing':
        completed = sum(d < ANCHOR for d in dates)
    first, last = dates[0].isoformat(), dates[-1].isoformat()
    add('classes', key=key, name=subject_data[subject][1] + ' Normal ' + key, subject=subject,
        type='Normal', coach=coach, room=room, max_capacity=capacity, min_capacity=minimum,
        price=subject_data[subject][3], status=state, sessions=13 if key == 'N10' else 12,
        first_session=first + 'T' + slots[slot][0], end_date=last,
        lifecycle='Đã xác nhận' if state in ['Ongoing','Closed'] else 'Chưa xác nhận; không tự khai giảng',
        postponements=1 if key == 'N04' else 0)
    add('normal_class_requests', key='REQ-' + key, sports_class=key, center='C01',
        request_id=str(uuid.uuid5(uuid.NAMESPACE_URL,'https://scms.demo.invalid/normal/'+key)))
    for member in members:
        enrollment = key + '-' + member
        invoice = 'INV-' + enrollment
        enrollment_state = 'Completed' if state == 'Closed' else 'Cancelled' if state == 'Cancelled' else 'Registered'
        receipt_day=min(dates[0]-timedelta(days=7),ANCHOR-timedelta(days=7)).isoformat()
        add('class_enrollments', key=enrollment, member=member, sports_class=key, invoice=invoice,
            status=enrollment_state, paid=True, enrolled_at=receipt_day+'T08:00')
        add('invoices', key=invoice, member=member, sports_class=key, subscription=None,
            amount=subject_data[subject][3], method='Cash', status='Paid',
            created_at=receipt_day+'T08:00', payment_date=receipt_day+'T08:01', receptionist='R1',
            reconciliation_required=False, cash_confirmation_reversed=False, gateway_ref=None)
    for index, d in enumerate(dates, 1):
        status = 'Cancelled' if state == 'Cancelled' else 'Completed' if index <= completed else 'Scheduled'
        add('sessions', key=f'{key}-S{index:02}', sports_class=key, date=d.isoformat(),
            start=slots[slot][0], end=slots[slot][1], coach=coach, room=room, member=None,
            status=status, finalized=status=='Completed', review_pending=False,
            center_invalidated=False, makeup_for=None)
    if key == 'N10':
        original = next(s for s in tables['sessions'] if s['key']=='N10-S08')
        original['center_invalidated'] = True
        original['status'] = 'Completed'
        makeup_date = dates_from(10, [3], 1)[0].isoformat()
        next(c for c in tables['classes'] if c['key']==key)['end_date']=max(last,makeup_date)
        add('sessions', key='N10-MU01', sports_class=key, date=makeup_date, start=slots[7][0], end=slots[7][1],
            coach=coach, room=room, member=None, status='Scheduled', finalized=False,
            review_pending=False, center_invalidated=False, makeup_for='N10-S08')
        add('center_incidents', key='INC-N10', session='N10-S08', status='Resolved',
            reason='DEMO buổi không tổ chức do trung tâm', evidence='DEMO xác minh buổi không tổ chức',
            session_compensated=True, created_by='CM1')
        for member in members:
            add('incident_affected_members', incident='INC-N10', member=member)

# Advanced stays on the existing D03/D10 model; technical IDs are resolved on execution.
for key,member,subject,state in [('AR01','M14','SWIM','Requested'),('AR02','M15','PICK','Accepted'),
                               ('AR03','M16','ARC','Rejected')]:
    add('advanced_registrations', key=key, member=member, subject=subject, coach=subject+'-A',
        status=state, subscription=None, request_key='DEMO-'+key,
        reason='DEMO Coach từ chối yêu cầu' if state=='Rejected' else None)
advanced=[('SUB01','M02','TEN','Active',12,-4),('SUB02','M03','TEN','Active',10,-5),
          ('SUB03','M04','BAD','Active',0,-6),('SUB04','M05','TEN','Completed',0,-40),
          ('SUB05','M06','TEN','Expired',0,-70),('SUB06','M07','BAD','Active',12,-2),
          ('SUB07','M08','SWIM','Cancelled',0,-3)]
template_by_subject={r['subject']:r['key'] for r in tables['classes'] if r['type']=='Advanced'}
for index,(key,member,subject,state,credit,paid_offset) in enumerate(advanced,4):
    coach=subject+'-A'
    amount=subject_data[subject][4]
    invoice='INV-'+key
    add('member_packages', key=key, member=member, subject=subject, primary_coach=coach,
        package=None, status=state, remaining_credit=credit, purchase_amount=amount,
        activated_at=None if key=='SUB07' else stamp(paid_offset),
        expires_at=None if key=='SUB07' else stamp(paid_offset+45),
        validity_days=45, note='DEMO Cash đã ghi nhận nhầm nhưng chưa nhận tiền' if key=='SUB06' else None)
    add('invoices', key=invoice, member=member, sports_class=None, subscription=key,
        amount=amount, method='VNPay' if key=='SUB07' else 'Cash', status='Paid',
        created_at=stamp(paid_offset,'07:50'), payment_date=stamp(paid_offset),
        receptionist=None if key=='SUB07' else 'R1', reconciliation_required=key=='SUB07',
        cash_confirmation_reversed=False, gateway_ref='DEMO-LATE-RECEIPT' if key=='SUB07' else None)
    add('advanced_registrations', key=f'AR{index:02}', member=member, subject=subject, coach=coach,
        status='Invoiced', subscription=key, request_key='DEMO-'+key, reason=None)
    session_dates=[]
    if key=='SUB02':session_dates=dates_from(2,[4,6],2)
    if key=='SUB03':session_dates=dates_from(1,[2,4,6],12)
    if key=='SUB04':session_dates=dates_from(-35,[2,4,6],12)
    if key=='SUB05':session_dates=dates_from(-65,[2,4,6],2)
    for number,d in enumerate(session_dates,1):
        completed=state in ['Completed','Expired']
        add('sessions', key=f'{key}-S{number:02}', sports_class=template_by_subject[subject],
            date=d.isoformat(), start=slots[7][0], end=slots[7][1], coach=coach,
            room='C01-'+subject+'-01', member=member, status='Completed' if completed else 'Scheduled',
            finalized=completed, review_pending=False, center_invalidated=False,
            makeup_for=None, subscription=key)
        if completed:
            add('attendances', key=f'ATT-{key}-{number:02}', session=f'{key}-S{number:02}',
                member=member, state='Present', note=None, check_in_time=None,
                absence_notice=None, superseded_at=None, recorded_by=coach)
            add('training_results', key=f'RESULT-{key}-{number:02}', session=f'{key}-S{number:02}',
                member=member, coach=coach, attendance_status='Present', content='DEMO bài tập cá nhân đã thực hiện')
add('training_plans', key='PLAN-SUB02', sports_class=None, member='M03', coach='TEN-A',
    title='DEMO giáo án cá nhân Advanced',content='Thực hành kỹ thuật riêng cho Member M03')
add('payment_resolutions', key='CC01', type='CashCorrection', scope='ADVANCED_UNUSED',
    invoice='INV-SUB06', enrollment=None, event=None, member='M07', status='Approved',
    amount=subject_data['BAD'][4], origin='COUNTER_MISTAKE', receiver=None, recipient_version=None,
    approved_by='CM1', completed_by=None, contact_phone=None,
    sla='Không phải Refund; chưa nhận tiền; Complete sẽ đảo xác nhận và thu hồi12credit theo D16')

for session in list(tables['sessions']):
    if session['status'] != 'Completed':
        continue
    if session.get('member') is not None:
        continue  # Private histories were added with their own original subscription above.
    members = paid_members[session['sports_class']]
    for index, member in enumerate(members):
        absent = member=='M03' and session['key'].endswith(('S02','S04'))
        key='ATT-' + session['key'] + '-' + member
        add('attendances', key=key, session=session['key'], member=member,
            state='Absent' if absent else 'Present', note='DEMO cần ôn lại kỹ thuật' if absent else None,
            check_in_time=None, absence_notice=None, superseded_at=None, recorded_by=session['coach'])
        if not absent and not session['center_invalidated']:
            add('training_results', key='RESULT-' + session['key'] + '-' + member,
                session=session['key'], member=member, coach=session['coach'], attendance_status='Present',
                content='DEMO thực hiện bài tập và được nhận xét tại buổi học')
for member in paid_members['N09']:
    add('evaluations', key='EVAL-N09-' + member, member=member, coach='TT-A',
        date=day(-15), score=8, comment='DEMO tiến bộ sau khóa Bóng bàn')
    add('class_feedbacks', key='FB-N09-' + member, member=member, sports_class='N09',
        rating=4, comment='DEMO phản hồi khóa đã hoàn tất')
for key in ['N08','N09','N10']:
    course=next(c for c in tables['classes'] if c['key']==key)
    add('training_plans', key='PLAN-' + key, sports_class=key, member=None, coach=course['coach'],
        title='DEMO giáo án lớp ' + key, content='Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng')

# Alternative proposal, individual choices and the single compatible target.
add('normal_class_events', key='EV-N05', sports_class='N05', kind='TRANSFER_OFFER', target_class='N11',
    status='WaitingResponses', actor='CM1', reason='DEMO lớp gốc không thể tổ chức theo kế hoạch')
add('normal_class_events', key='EV-N04', sports_class='N04', kind='POSTPONE', target_class=None,
    status='Applied', actor='CM1', reason='DEMO đã dời một lần vì thiếu sĩ số')
for member in paid_members['N04']:
    add('normal_member_responses', event='EV-N04', member=member, choice='Accepted', source='MEMBER_SESSION')
for member, choice in [('M01','WaitingResponse'),('M02','RefundRequested'),('M03','WaitingResponse')]:
    add('normal_member_responses', event='EV-N05', member=member, choice=choice, source='MEMBER_SESSION' if choice!='WaitingResponse' else None)
notes['normal_enrollment_transfers']='Ban đầu rỗng. Member M01 xác nhận thực hiện chuyển N05→N11 trong bước demo: giữ invoice gốc, recheck dưới khóa, N11 từ3/4→4/4. Accepted ở response là lựa chọn, chưa phải chuyển quyền đã commit.'
for key in ['N06','N12']:
    add('normal_class_events', key='EV-' + key, sports_class=key, kind='CANCEL_PRESTART', target_class=None,
        status='Applied', actor='CM1', reason='DEMO trung tâm không thể tổ chức trước buổi đầu')

refund_specs=[('RF01','N05','M02','AwaitingReceiver'),('RF02','N06','M04','AwaitingReceiver'),
              ('RF03','N06','M05','AwaitingApproval'),('RF04','N06','M06','Approved'),
              ('RF05','N12','M07','AwaitingVerification'),('RF06','N12','M08','Completed'),
              ('RF07','N12','M09','TransferIssue')]
for key, course, member, state in refund_specs:
    amount=next(c['price'] for c in tables['classes'] if c['key']==course)
    add('payment_resolutions', key=key, type='Refund', scope='NORMAL_PRESTART',
        invoice='INV-'+course+'-'+member, enrollment=course+'-'+member, event='EV-'+course,
        member=member, status=state, amount=amount, origin='MEMBER_REQUEST' if course=='N05' else 'CENTER_CANCELLATION',
        receiver=None if state=='AwaitingReceiver' else 'DEMO-BANK / '+member+' / DEMO-ACCOUNT-'+member,
        recipient_version=0 if state=='AwaitingReceiver' else 1,
        approved_by='CM1' if state in ['Approved','AwaitingVerification','Completed','TransferIssue'] else None,
        approved_at=stamp(-3,'00:00') if state in ['AwaitingVerification','Completed','TransferIssue'] else stamp(0,'00:00') if state=='Approved' else None,
        receiver_confirmed_at=stamp(-3,'00:00') if state in ['AwaitingVerification','Completed','TransferIssue'] else stamp(0,'00:00') if state in ['Approved','AwaitingApproval'] else None,
        completed_by='CM2' if state=='Completed' else None,
        contact_phone='JOIN users.phone theo member; FE tùy chọn',
        sla='7ngày TT mở; chỉ tính khi Approved và đủ receiver')
    if state in ['AwaitingVerification','Completed','TransferIssue']:
        attempt='TR-'+key
        add('refund_transfer_attempts', key=attempt, resolution=key, actor='R1', amount=amount,
            recipient_version=1, bank_ref='DEMO-TRANSFER-'+key,
            status='VerifiedSuccess' if state=='Completed' else 'Unknown' if state=='TransferIssue' else 'Submitted',
            performed_at=stamp(-1))
        if state!='TransferIssue':
            add('refund_documents', key='DOC-'+key, attempt=attempt, uploader='R1',
                file_kind='PDF', storage='PRIVATE_DEMO_OBJECT_TO_GENERATE',
                description='Mẫu phải ghi DEMO; file chưa được upload/sinh ở lượt thiết kế này')
        if state=='Completed':
            add('refund_verifications', key='VER-'+key, attempt=attempt, manager='CM2',
                result='MatchedSuccess', source='DEMO sao kê trung tâm độc lập với ảnh Receptionist', verified_at=stamp(0))
add('notifications', key='NT01', user='M01', event='TRANSFER_OFFER', related='EV-N05', is_read=False,
    content='DEMO đề nghị chuyển N05→N11; chờ lựa chọn Member, không tự đồng ý')
add('notifications', key='NT02', user='M04', event='REFUND_AWAITING_RECEIVER', related='RF02', is_read=False,
    content='DEMO trung tâm hủy lớp; đã có nghĩa vụ hoàn, chờ thông tin nhận')
add('notifications', key='NT03', user='M08', event='REFUND_COMPLETED', related='RF06', is_read=True,
    content='DEMO đã xác minh hoàn900000đ; tham chiếu DEMO-TRANSFER-RF06')
for row in tables['notifications']:
    add('notification_recipients', notification=row['key'], user=row['user'], is_read=row['is_read'])
add('chat_conversations', key='CHAT01', member='M08', assigned_staff='R1')
add('chat_messages', key='MSG01', conversation='CHAT01', sender='M08', content='DEMO tôi muốn kiểm tra khoản hoàn RF06')
add('chat_messages', key='MSG02', conversation='CHAT01', sender='R1', content='DEMO trung tâm đã tiếp nhận để kiểm tra lại giao dịch')
add('chat_read_state', conversation='CHAT01', user='M08', last_read='MSG01')
add('chat_read_state', conversation='CHAT01', user='R1', last_read='MSG02')
for key, action, actor, target in [('LOG01','CANCEL_NORMAL_PRESTART','CM1','N06'),
                                    ('LOG02','APPROVE_NORMAL_REFUND','CM1','RF04'),
                                    ('LOG03','SUBMIT_REFUND_PROOF','R1','RF05'),
                                    ('LOG04','VERIFY_NORMAL_REFUND','CM2','RF06')]:
    add('system_logs', key=key, action=action, actor=actor, target=target,
        center='C01', category='BUSINESS', scope_source='TARGET_AT_EVENT', detail='DEMO; không chứa số tài khoản/file URL/phone')
add('system_logs', key='LOG05', action='UPDATE_ROLE_PERMISSIONS', actor='ADM', target='Coach',
    center=None, category='SYSTEM_SECURITY', scope_source='GLOBAL_CONFIGURATION_AT_EVENT',
    detail='DEMO Admin gửi lại tập quyền Coach baseline không đổi; log sẽ sinh bởi API')

notes.update({
 'users':'Giữ nguyên33account PDF. Mã tham chiếu không phải user_id API. Không xuất password/hash/QR secret. Account thêm dùng miền .invalid, không gửi email. Các cột runtime khác giữ DEFAULT/NULL phù hợp; role/center không từ client.',
 'roles':'GET roles/permissions/role detail chỉ Admin Active; quyền ghi không tự suy từ quyền đọc.',
 'role_permissions':'Baseline hiện hành; permission legacy có thể tồn tại nhưng không làm API legacy hoạt động trở lại. Không mở thêm Admin tài chính hoặc quyền Coach ngoài assignment.',
 'coach_work_slots':'Mỗi dòng là một slot thật của quan hệ. T2–T7 là lịch Coach demo, không phải định nghĩa ngày làm việc của TT; Chủ nhật trống để trình diễn cấu hình. Outdoor vẫn cấm ca3/4.',
 'classes':'Migration21/BE đã có phần lõi minimum/confirmation/start-close; dời/chuyển/hủy/Refund và fixture vẫn chưa hoàn tất theo checkpoint Phase1. Các dòng này là thiết kế, không là dữ liệu đã thực thi. Status Class không có Completed;9Advanced là template chưa có buổi, không cấp quyền học.',
 'sessions':'Mốc thiết kế cố định2026-11-10 00:00 VN. Script demo thực thi sau này phải chọn lại anchor hợp lệ, kiểm resource/Member conflict và TTL. Không tự sửa Clock Render để giữ kịch bản sống. N10 có13record/12hợp lệ; makeup giữ buổi gốc.',
 'invoices':'key ở đây là mã manifest, không phải invoiceCode gửi gateway. InvoiceCode thật do sequence không tái dùng cấp. Tất cả khoản này là dữ liệu mô phỏng được duyệt, không có thu/chuyển tiền thật hoặc callback gateway thật.',
 'payment_resolutions':'Tên state/field ở đây diễn đạt giai đoạn nghiệp vụ để thống nhất dữ liệu, chưa là enum/DTO/API đã triển khai. Receiver DEMO không phải tài khoản ngân hàng có thể chuyển tiền. Contact phone tùy FE dùng, không thay xác minh tài khoản.',
 'refund_documents':'Chỉ định nghĩa asset DEMO cần có khi thực hiện fixture; chưa tạo file/URL thật. Chứng từ phải private, không dùng avatar/public URL; Completed trong bản thiết kế giả định có đủ asset và xác minh mô phỏng khi seed sau này.',
 'normal_class_requests':'Mã REQ-* là key manifest; request_id ở bảng là UUID ổn định cho API. Request hash chỉ sinh từ payload chính xác khi thực hiện tạo lớp, không đoán trong tài liệu.',
 'member_packages':'Advanced theo D03/D10:12credit chỉ sau Paid hợp lệ, còn0credit nhưng có buổi chưa học vẫn Active. SUB07 nhận tiền muộn cần đối soát, không cấp quyền. Các dòng là kế hoạch fixture, cần thực hiện/kiểm qua workflow trước seed.',
 'advanced_registrations':'Giữ Requested/Accepted/Rejected/Invoiced riêng với state của Invoice/Subscription; không sửa Advanced theo file Normal.',
 'email_otps':'Rỗng ban đầu. Demo đăng ký/reset password tạo OTP tại thời điểm chạy; không seed/export OTP hoặc secret.',
 'vnpay_payment_attempts':'Rỗng ban đầu. Chỉ sinh khi tạo link pay đúng workflow; không backfill metadata/paydate hoặc bật QueryDR để lấp dữ liệu.',
 'membership_packages':'Rỗng và bị chặn ghi theo mô hình retire; không có sản phẩm tháng/VIP.',
 'support_requests':'Rỗng: chưa có workflow ticket được nghiệm thu; dùng chat hiện có cho khiếu nại demo.',
 'ai_consultations':'Rỗng: AI chưa hoàn tất, không tự tạo lịch sử như thể provider đã chạy.',
 'scms_data_migration_audit':'Marker do migration/reset thực tế tạo, không seed giả migration đã chạy trong bộ thiết kế này.',
 'session_absence_notices':'Case Advanced sát giờ cần ca thật tại thời điểm demo; không mở báo vắng cá nhân Normal.',
 'session_changes':'Thao tác dời buổi/thay Coach tạo trong bước demo qua API; không seed history để giả đã đi qua action.',
 'subscription_pauses':'Tạo từ incident/ngày nghỉ có nguồn khi chạy fixture Advanced; không thêm pause độc lập để làm đẹp deadline.',
})

# Every existing table has its own section, even when intentionally empty or deferred.
builder=ast.parse((ROOT/'scripts/build-demo-schema.py').read_text(encoding='utf-8'))
scope=next(ast.literal_eval(node.value.func.value).split() for node in builder.body
           if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='TABLES' for t in node.targets))
existing=[name for name in scope if '_backup_' not in name]
new=['normal_class_events','normal_member_responses','normal_enrollment_transfers',
     'refund_transfer_attempts','refund_documents','refund_verifications']
for table in existing+new:
    tables.setdefault(table,[])


def validate():
    assert len(accounts)==33 and len(tables['users'])==47
    assert len(tables['classes'])==21 and len(tables['rooms'])==20
    assert len(existing)>=47 and set(tables)==set(existing+new)
    user_keys={r['key'] for r in tables['users']}
    class_keys={r['key'] for r in tables['classes']}
    room_keys={r['key'] for r in tables['rooms']}
    role_keys={r['key'] for r in tables['roles']}
    subject_keys_set={r['key'] for r in tables['subjects']}
    permission_keys={r['key'] for r in tables['permissions']}
    for user in tables['users']:assert user['role'] in role_keys
    for row in tables['role_permissions']:
        assert row['role'] in role_keys and row['permission'] in permission_keys
    assert len({(r['coach'],r['slot_key']) for r in tables['coach_work_slots']})==len(tables['coach_work_slots'])
    for key,rows in tables.items():
        keys=[r['key'] for r in rows if 'key' in r]
        assert len(keys)==len(set(keys)), key
    for row in tables['class_enrollments']:
        assert row['member'] in user_keys and row['sports_class'] in class_keys
    assert len({(r['member'],r['sports_class']) for r in tables['class_enrollments']})==len(tables['class_enrollments'])
    for row in tables['normal_class_requests']:
        assert str(uuid.UUID(row['request_id']))==row['request_id']
    for row in tables['member_packages']:
        assert row['member'] in user_keys and row['subject'] in subject_keys_set and row['primary_coach'] in user_keys
    for row in tables['sessions']:
        assert row['sports_class'] in class_keys and row['room'] in room_keys and row['coach'] in user_keys
        if row.get('subscription'):
            sub=next(s for s in tables['member_packages'] if s['key']==row['subscription'])
            ends=datetime.fromisoformat(row['date']+'T'+row['end'])
            assert datetime.fromisoformat(sub['activated_at'])<=ends<=datetime.fromisoformat(sub['expires_at'])
    for row in tables['classes']:
        if row['type']=='Normal':
            assert (2*row['max_capacity']+2)//3<=row['min_capacity']<=row['max_capacity']
            valid=[s for s in tables['sessions'] if s['sports_class']==row['key'] and not s['center_invalidated']]
            assert len(valid)==12
            assert len(paid_members[row['key']])<=row['max_capacity']
    source=next(c for c in tables['classes'] if c['key']=='N05')
    target=next(c for c in tables['classes'] if c['key']=='N11')
    assert source['subject']==target['subject']=='PICK' and source['price']==target['price']==1800000
    assert len(paid_members['N11'])==3
    assert all(s['date']<ANCHOR.isoformat() for s in tables['sessions'] if s['status']=='Completed')
    assert all(s['date']>=ANCHOR.isoformat() for s in tables['sessions'] if s['status']=='Scheduled')
    active=[s for s in tables['sessions'] if s['status']!='Cancelled' and not s['center_invalidated']]
    conflicts=[]
    for index,a in enumerate(active):
        for b in active[index+1:]:
            if a['date']!=b['date'] or a['start']>=b['end'] or a['end']<=b['start']:
                continue
            a_members={a['member']} if a['member'] else set(paid_members[a['sports_class']])
            b_members={b['member']} if b['member'] else set(paid_members[b['sports_class']])
            if a['coach']==b['coach'] or a['room']==b['room'] or a_members & b_members:
                conflicts.append((a['key'],b['key']))
    assert not conflicts, conflicts[:8]
    for row in tables['payment_resolutions']:
        invoice=next(i for i in tables['invoices'] if i['key']==row['invoice'])
        assert invoice['status']=='Paid' and invoice['amount']==row['amount'] and invoice['member']==row['member']
    for row in tables['training_results']:
        session=next(s for s in tables['sessions'] if s['key']==row['session'])
        assert session['status']=='Completed' and not session['center_invalidated']
    holiday_dates={h['date'] for h in tables['holidays'] if h['is_active']}
    for row in tables['payment_resolutions']:
        if row['type']!='Refund' or not row.get('approved_at'):
            continue
        cursor=datetime.fromisoformat(max(row['approved_at'],row['receiver_confirmed_at'])).date()
        counted=0
        while counted<7:
            cursor+=timedelta(days=1)
            if cursor.isoformat() not in holiday_dates:counted+=1
        row['transfer_due_at']=cursor.isoformat()+'T23:59:59.999'


def cell(value):
    if value is None:return 'NULL'
    if value is True:return 'true'
    if value is False:return 'false'
    return str(value).replace('|','\\|').replace('\n','<br>')


validate()
if args.manifest:
    Path(args.manifest).write_text(json.dumps({'anchor':ANCHOR.isoformat(),'tables':tables},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parts=['''# SYSTEM DATA — Bộ dữ liệu gốc SCMS được duyệt10/11/2026

**Trạng thái: DATA DESIGN, CHƯA SEED/RESET SUPABASE.** File này ghi bộ tham chiếu và các dòng thiết kế cho FE/BE, không phải dump DB đang chạy hoặc bằng chứng API Normal/Refund mới đã có. Các bảng mới/field nghiệp vụ là dự kiến kỹ thuật, chưa là contract đã triển khai. ID DB được sinh khi seed; `key`, Nxx/Mxx/RFxx là mã đối chiếu manifest, FE phải lấy ID thật từ API.

Nguồn: [Owner decisions](docs/04-DEBUG-NOTE.md), [review và phê duyệt](docs/03-TODO.md#system-data-review-20261109), [Normal Lifecycle](SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Refund Normal](SCMS_BE_Refund_Normal_DA_CHOT.md), reference seed đã đối chiếu33account PDF. [Active/QA](docs/00-MASTER-CHECKLIST.md#active) giữ trạng thái kiểm/phát hành; dữ liệu đã đọc trên cloud không được trộn vào bộ mới này.

## Quy ước

- Anchor thiết kế:2026-11-10 00:00 Asia/Ho_Chi_Minh, không phải cấu hình Clock cho môi trường deploy. Fixture thực thi phải chọn lại ngày và kiểm conflict/ràng buộc trước commit.
- Giữ33account PDF, thêm14account riêng. Mật khẩu demo theo quyết định Owner; không đưa hash/secret vào bảng. Chỉ account demo bỏ first-login; không đổi policy tạo account thực qua API.
- 9môn/20room/9template Advanced/12Normal; N05→N11 Pickleball cùng1.800.000đ/center1, Member đồng ý rõ. Không tăng số lớp để bổ sung đích.
- Ngày làm việc hoàn tiền = ngày TT mở theo Holiday, tính T7/CN nếu không đóng; giữ7ngày làm việc. Lịch nghỉ Coach không dừng SLA của trung tâm.
- Contact phone của Member là dữ liệu liên hệ đúng quyền, FE quyết định dùng/hiển thị; không nằm trong chứng từ ngân hàng và không thay xác minh.
- Mỗi bảng có mục riêng. Bảng rỗng nêu rõ vì sao; các scenario động/Advanced chưa cụ thể hóa nằm ở phần còn lại, không được xem là đã có đủ dòng để thực thi toàn bộ demo.
- Dữ liệu tài chính/chứng từ đều là mô phỏng có nhãn DEMO; không tạo reference giả giống giao dịch ngân hàng thật, không gọi mail/gateway/ngân hàng. Unlisted nullable/runtime columns dùng DEFAULT/NULL theo schema khi triển khai; bảng này không cung cấp secret.

## Các bảng dữ liệu
''']
for index,table in enumerate(existing+new,1):
    rows=tables[table]
    parts.append(f'\n### {index}. `{table}`'+(' — bảng mới dự kiến' if table in new else '')+'\n')
    if table in notes:parts.append(notes[table]+'\n')
    if not rows:
        parts.append('Chưa có dòng tĩnh trong bản thiết kế này.\n')
        continue
    columns=list(dict.fromkeys(k for row in rows for k in row))
    parts.append('| '+' | '.join(columns)+' |\n| '+' | '.join(['---']*len(columns))+' |')
    for row in rows:parts.append('| '+' | '.join(cell(row.get(k)) for k in columns)+' |')
parts.append('''
## Phần cần hoàn thiện trước khi dùng như bộ dữ liệu thực thi

Đây chưa phải bản xuất toàn bộ row cuối cùng từ script seed. Phần tham chiếu/account/lớp, lịch Normal và các dòng minh họa được liệt kê để phối hợp FE; phần dưới cần triển khai/kiểm rồi bổ sung vào cùng file, không tự tính là đã cover đủ mọi flow.

| Nhóm | Phần phải bổ sung/kiểm khi hiện thực fixture |
|---|---|
| Advanced | Các dòng Requested/Accepted/Rejected/Invoiced và quyền Active/Completed/Expired đã có ở bảng thiết kế; Pending0credit tạo tại bước demo để không seed hold hết hạn. Từng dòng phải được hiện thực/kiểm qua workflow, đúng45ngày và nguồn ngày nghỉ. |
| Normal động | Pending5phút tạo lúc demo, waitlist/chỗ cuối/race, Expired/Failed/Cancelled, callback muộn và Paid cần đối soát; chưa seed các dòng này như thể đã có giao dịch thật. |
| Attendance động | Trong buổi/sau end/cutoff, note xóa/readback, Coach A/B và Manager sau hạn; QR/OTP chỉ tạo khi chạy. |
| Incident/thay Coach | Session changes cho thao tác dời/thay Coach, notice Advanced/superseded và pause có nguồn; dùng API sau khi triển khai, không ghi lại lịch sử bằng phỏng đoán. |
| Refund | Private asset thật mang chữ DEMO, version người nhận/approve/claim/verification và timestamp đủ thứ tự; trạng thái trong bảng hiện là giai đoạn nghiệp vụ, chưa có enum/API mới. |
| SQL/manifest | Sinh ID/mã hóa đơn/requestId đúng kiểu, FK toàn bộ và số dư sau commit; không reset invoice_code_seq. Kiểm lại lịch N05→N11 và capacity khi chuyển. |
| FE/live | FE quyết định UI, đối chiếu ID/response thật sau deploy. Không suy thiết kế dữ liệu, OpenAPI hoặc local tests thành FE/Render/CRUD/live đã nghiệm thu. |

Sau khi hoàn tất fixture: chạy kiểm local cô lập, xuất lại dữ liệu từ đúng manifest đã kiểm và thay phần DATA DESIGN bằng trạng thái đã seed tương ứng. Không chạy bản reset tham chiếu rỗng chỉ vì file này đã được tạo.
''')
data=('\n'.join(parts)+'\n').encode('utf-8')
output=ROOT/'SYSTEM-DATA.md'
if args.from_local_pg:
    import subprocess
    database=args.from_local_pg
    assert re.fullmatch(r'scms_schema_demo_phase1_[a-z0-9_]+',database),'Disposable fixture DB only'
    container='scms-phases-20261027'
    config=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
    assert config['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':'55440'}]
    def sql(query):
        return subprocess.check_output(['docker','exec',container,'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','postgres','-d',database,'-c',query],text=True,encoding='utf-8').strip()
    assert sql('SELECT count(*) FROM users')=='47' and sql('SELECT count(*) FROM classes')=='21'
    assert sql('SELECT count(*) FROM normal_refund_documents')=='2'
    header='''# SYSTEM DATA — Bộ demo SCMS đã duyệt

**DATA DESIGN — fixture đã thực thi và kiểm trên PostgreSQL local cô lập; CHƯA SEED/RESET SUPABASE.**
Đây là dữ liệu demo tổng hợp có nhãn DEMO, không phải dữ liệu/tiền/chứng từ ngân hàng thật hoặc bằng chứng nghiệp vụ cloud. Bảng bên dưới được xuất từ đúng fixture SQL đã chạy local, gồm các cột thực tế; không còn sáu tên bảng dự kiến đứng song song với schema.

Nguồn: [Normal Lifecycle](SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Normal Refund](SCMS_BE_Refund_Normal_DA_CHOT.md), [schema/generator](database/README.md), [kết quả và giới hạn](docs/00-MASTER-CHECKLIST.md#active).
Giữ 33 account PDF và thêm 14 account, 9 môn, 20 sân, 9 template Advanced và 12 lớp Normal. N05/N11 được nhận diện theo tên lớp; FE lấy ID thật từ API. Invoice lớp mua/amount giữ nguyên khi chuyển, entitlement_class_id chỉ lớp nhận quyền hiện tại.

Mốc ngày VN của fixture: ANCHOR_DATE. Khi chuẩn bị chạy ở ngày khác, tái sinh cả fixture/schema và kiểm lại lịch trước khi dùng; không đổi Clock Render hoặc kéo dài TTL. Hạn hoàn là hết ngày TT mở thứ bảy tiếp theo; tính cả T7/CN trừ Holiday. Contact phone chỉ cho tài chính đúng quyền, FE quyết định hiển thị.

Mọi lịch sử học/thu/hoàn ở đây là ví dụ tổng hợp được duyệt, không khẳng định đã thực hiện ngoài SCMS. Hai PDF nằm trong normal_refund_documents.content riêng tư, có chữ DEMO; bảng chỉ xuất kích thước/hash. Không xuất password/hash, OTP, QR secret, cookie, token hoặc nội dung binary.

## Dữ liệu thực thi theo bảng
'''.replace('ANCHOR_DATE',ANCHOR.isoformat())
    actual=[header]
    for index,table in enumerate(existing,1):
        assert re.fullmatch(r'[a-z_]+',table)
        query="SELECT coalesce(jsonb_agg(j ORDER BY j::text),'[]'::jsonb) FROM (SELECT to_jsonb(t)-ARRAY['password_hash','checkin_secret','checkin_last_step','google_sub','content']||CASE WHEN '"+table+"'='normal_refund_documents' THEN jsonb_build_object('private_content','OMITTED; authenticated download only') ELSE CASE WHEN to_jsonb(t) ? 'content' THEN jsonb_build_object('content',to_jsonb(t)->'content') ELSE '{}'::jsonb END END AS j FROM public."+table+" t) x"
        rows=json.loads(sql(query))
        actual.append(f'\n### {index}. `{table}`\n')
        if not rows:
            reasons={'normal_class_requests':'Fixture SQL tạo lớp trực tiếp có nhãn DEMO; không giả idempotency request/hash của API.',
              'normal_transfers':'Chuyển N05→N11 do Member xác nhận qua API trong demo; M01/M03 đang Waiting, preview không giữ chỗ.',
              'email_otps':'OTP chỉ tạo tại thời điểm đăng ký/reset demo, không seed secret.',
              'vnpay_payment_attempts':'Chỉ tạo khi phát hành link VNPay trong workflow local; không seed metadata provider.',
              'membership_packages':'Mô hình legacy đã retire; không tái tạo catalog gói chung.'}
            actual.append('Rỗng. '+reasons.get(table,'Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.')+'\n');continue
        columns=list(rows[0]);actual.append('| '+' | '.join(columns)+' |\n| '+' | '.join(['---']*len(columns))+' |')
        for row in rows:actual.append('| '+' | '.join(cell(row.get(k)) for k in columns)+' |')
    actual.append('''
## Kịch bản động và giới hạn

- Normal Pending/Expired/waitlist/chỗ cuối và callback đến muộn tạo tại lúc chạy bằng API; không seed giữ chỗ đã hết hạn. Bộ kiểm HTTP local kiểm commit/race, không gọi gateway thật.
- N05 có phương án chuyển N11: M01/M03 đang chờ phản hồi; M02 đã chọn Refund và mất quyền ở lớp gốc. Accept phải commit transfer cùng linkage; không dùng nhãn Accepted khi quyền chưa chuyển.
- Refund gồm chờ receiver/duyệt/Approved/chờ xác minh/Completed/sự cố chưa rõ; Completed có attempt, private PDF và nguồn đối soát DEMO độc lập. Chứng từ upload không tự Completed. Phiên bản receiver và verification được giữ riêng, lịch sử không bị ghi đè.
- Incident N10 giữ buổi gốc invalidated và buổi bù Scheduled; lớp Ongoing. Attendance cũ là dữ liệu tổng hợp; QR/OTP và cửa sổ correction cần ca đúng thời điểm khi demo.
- Advanced giữ 45 ngày/12 credit; Expired có credit bị thu hồi, Active 0 credit vẫn có buổi đã đặt. CashCorrection không là tiền hoàn.
- R1/R2, FE/browser, cloud/deploy, QueryDR/provider thật và Google/avatar thuộc lượt tiếp theo, chưa nghiệm thu ở đây.
''')
    data=('\n'.join(actual)+'\n').encode('utf-8')
if args.no_render:
    pass
elif args.check:
    assert output.read_bytes()==data,'SYSTEM-DATA.md is stale; run build-system-data.py'
else:
    output.write_bytes(data)
print(json.dumps({'file':'SYSTEM-DATA.md','mode':'DATA_DESIGN_NOT_SEEDED','tables':len(existing) if args.from_local_pg else len(tables),
                  'users':len(tables['users']),'classes':len(tables['classes']),
                  'sha256':hashlib.sha256(data).hexdigest(),'databaseAccess':bool(args.from_local_pg),'cloudAccess':False}))
