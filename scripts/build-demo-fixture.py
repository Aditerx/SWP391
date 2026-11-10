"""Compile the approved DATA DESIGN into SQL. No database/network access; all bank assets say DEMO.

The rebuild owns these rows. Historical sessions are synthetic examples, not evidence of real classes.
Pending/OTP/QR/provider attempts are created by the local HTTP scenarios, never kept alive by a seed.
"""
from pathlib import Path
from datetime import date, datetime, timedelta
from io import BytesIO
import argparse, contextlib, hashlib, json, runpy, sys, uuid

ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--anchor',required=True)
p.add_argument('--output',default='database/bootstrap/scms_demo_scenarios.sql')
p.add_argument('--preview-dir',help='Optional local PDF preview directory; assets themselves are private BYTEA rows')
a=p.parse_args()
anchor=date.fromisoformat(a.anchor)
old=sys.argv
try:
    sys.argv=['build-system-data.py','--anchor',a.anchor,'--no-render']
    with contextlib.redirect_stdout(BytesIOText:=__import__('io').StringIO()):
        design=runpy.run_path(str(ROOT/'scripts/build-system-data.py'))
finally:sys.argv=old
t=design['tables']; statements=[]
def q(value):
    if value is None:return 'NULL'
    if isinstance(value,bool):return 'TRUE' if value else 'FALSE'
    if isinstance(value,(int,float)):return str(value)
    return "'"+str(value).replace("'","''")+"'"
def raw(text):return ('SQL',text)
def insert(table,**row):
    def value(v):return v[1] if isinstance(v,tuple) else q(v)
    statements.append('INSERT INTO '+table+'('+','.join(row)+') VALUES('+','.join(value(v) for v in row.values())+');')
def stamp(offset=0,hour=None):return (anchor+timedelta(days=offset)).isoformat()+' '+(hour or ('00:00' if offset==0 else '08:00'))
user={r['key']:(r['pdf_ordinal'] or 34+index) for index,r in enumerate([u for u in t['users'] if u['pdf_ordinal'] is None])}
user.update({r['key']:r['pdf_ordinal'] for r in t['users'] if r['pdf_ordinal']})
subject={r['key']:raw('(SELECT subject_id FROM subjects WHERE slug='+q(r['slug'])+')') for r in t['subjects']}
room={r['key']:raw('(SELECT room_id FROM rooms WHERE room_code='+q(r['key'])+')') for r in t['rooms']}
classes={r['key']:i for i,r in enumerate(t['classes'],1)}
sessions={r['key']:i for i,r in enumerate(t['sessions'],1)}
subs={r['key']:i for i,r in enumerate(t['member_packages'],1)}
invoices={r['key']:i for i,r in enumerate(t['invoices'],1)}
enroll={r['key']:i for i,r in enumerate(t['class_enrollments'],1)}
resolutions={r['key']:i for i,r in enumerate(t['payment_resolutions'],1)}
course={r['key']:r for r in t['classes']}
def obligation_created(key):
    inv=next(i for i in t['invoices'] if i['key']==key)
    first=datetime.fromisoformat(course[inv['sports_class']]['first_session']) if inv['sports_class'] else datetime.fromisoformat(stamp())
    return max(datetime.fromisoformat(inv['payment_date'])+timedelta(hours=1),min(first-timedelta(days=2),datetime.fromisoformat(stamp(-3,'00:00'))))

statements += [f"-- Approved synthetic demo fixture. Anchor {anchor} Asia/Ho_Chi_Minh. No real bank/payment events.",
 f"DO $anchor$ BEGIN IF CURRENT_DATE<>DATE '{anchor}' THEN RAISE EXCEPTION 'SCMS_DEMO_STALE_ANCHOR: regenerate the bundle for the intended VN date'; END IF; END $anchor$;",
 # These two named time guards use wall-clock CURRENT_DATE. All FK/CHECK/unique constraints stay active.
 "ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;",
 "ALTER TABLE sessions DISABLE TRIGGER trg_sessions_validate;",
 "SELECT setval('invoice_code_seq',greatest((SELECT value FROM _scms_code_floor),(SELECT last_value FROM invoice_code_seq)),true);"]
for r in t['users']:
    if r['pdf_ordinal']:continue
    insert('users',user_id=user[r['key']],role_id=raw('(SELECT role_id FROM roles WHERE role_name='+q(r['role'])+')'),
           full_name=r['name'],email=r['email'],password_hash=raw("extensions.crypt('SWP391',extensions.gen_salt('bf',10))"),status=r['status'],center_id=1,is_first_login=False)
    insert('coaches' if r['role']=='Coach' else 'members',user_id=user[r['key']],**({} if r['role']=='Coach' else {'join_date':anchor.isoformat()}))
statements.append("INSERT INTO coach_specializations(coach_id,specialization_id) SELECT "+str(user['CI'])+",specialization_id FROM specializations WHERE subject_id="+subject['BAD'][1]+";")
statements.append("DELETE FROM coach_work_slots WHERE slot_key LIKE 'SUNDAY:%';")
for r in t['holidays']:insert('holidays',holiday_date=r['date'],holiday_name=r['name'],tag=r['tag'],is_active=r['is_active'])
for r in t['coach_days_off']:insert('coach_days_off',coach_id=user[r['coach']],day_off=r['date'])
for r in t['classes']:
    if r['type']=='Advanced':continue
    confirmed=r['status'] in ('Ongoing','Closed')
    first=datetime.fromisoformat(r['first_session']); confirmed_at=(first-timedelta(days=2)).isoformat()
    insert('classes',class_id=classes[r['key']],class_name=r['name'],subject_id=subject[r['subject']],coach_id=user[r['coach']],
           max_capacity=r['max_capacity'],min_capacity=r['min_capacity'],tuition_fee=r['price'],status=r['status'],class_type='Normal',total_sessions=12,
           center_id=1,pricing_draft=False,start_date=first.date().isoformat(),end_date=r['end_date'],opening_confirmed_by=user['CM1'] if confirmed else None,
           opening_confirmed_at=confirmed_at if confirmed else None,opening_reason='DEMO confirmed opening' if confirmed else None,
           postponement_count=r['postponements'],lifecycle_version=1 if confirmed or r['status']=='Cancelled' or r['key'] in ('N04','N05') else 0,change_pending=r['key']=='N05')
for r in t['member_packages']:
    inv=next(i for i in t['invoices'] if i['subscription']==r['key']);paid=datetime.fromisoformat(inv['payment_date'])
    insert('member_packages',subscription_id=subs[r['key']],member_id=user[r['member']],subject_id=subject[r['subject']],primary_coach_id=user[r['primary_coach']],
           start_date=paid.date().isoformat(),end_date=(paid+timedelta(days=45)).date().isoformat(),remaining_credit=r['remaining_credit'],validity_days=45,
           status=r['status'],purchase_amount=r['purchase_amount'],activated_at=r['activated_at'],expires_at=r['expires_at'],expiry_revoked_credit=10 if r['key']=='SUB05' else 0,expiry_processed_at=r['expires_at'] if r['key']=='SUB05' else None)
for r in t['invoices']:
    insert('invoices',invoice_id=invoices[r['key']],member_id=user[r['member']],class_id=classes.get(r['sports_class']),subscription_id=subs.get(r['subscription']),
           amount=r['amount'],invoice_code=raw("'INV-'||extract(year FROM DATE "+q(anchor.isoformat())+")::int||'-'||nextval('invoice_code_seq')"),
           payment_method=r['method'],payment_status=r['status'],created_at=r['created_at'],payment_date=r['payment_date'],expires_at=None,
           receptionist_id=user.get(r['receptionist']),reconciliation_required=r['reconciliation_required'],cash_confirmation_reversed=False,gateway_transaction_ref=r['gateway_ref'])
for r in t['class_enrollments']:
    # An obligation withdraws the right immediately, before any external transfer.
    has_refund=any(x.get('enrollment')==r['key'] and x['type']=='Refund' for x in t['payment_resolutions'])
    insert('class_enrollments',enrollment_id=enroll[r['key']],class_id=classes[r['sports_class']],member_id=user[r['member']],status='Cancelled' if has_refund else r['status'],enrolled_at=r['enrolled_at'],
           cancelled_at=obligation_created(r['invoice']).isoformat() if has_refund else None,cancelled_by=user['M02'] if r['sports_class']=='N05' and has_refund else user['CM1'] if has_refund else None,cancel_reason='DEMO center refund obligation' if has_refund else None)
for r in t['advanced_registrations']:
    insert('advanced_registrations',member_id=user[r['member']],subject_id=subject[r['subject']],coach_id=user[r['coach']],request_key=r['request_key'],status=r['status'],
           created_at=stamp(-5),accepted_by=user[r['coach']] if r['status'] in ('Accepted','Invoiced') else None,accepted_at=stamp(-4) if r['status'] in ('Accepted','Invoiced') else None,reason=r['reason'],subscription_id=subs.get(r['subscription']))
for r in t['sessions']:
    insert('sessions',session_id=sessions[r['key']],class_id=classes[r['sports_class']],room_id=room[r['room']],session_date=r['date'],start_time=r['start'],end_time=r['end'],
           coach_id=user[r['coach']],booked_member_id=user.get(r['member']),subscription_id=subs.get(r.get('subscription')),status=r['status'],attendance_finalized=r['finalized'],attendance_review_pending=False,
           center_invalidated=r['center_invalidated'],makeup_for_session_id=sessions.get(r['makeup_for']),created_by=user['CM1'])
for r in t['attendances']:
    insert('attendances',session_id=sessions[r['session']],member_id=user[r['member']],recorded_by=user[r['recorded_by']],state=r['state'],note=r['note'])
for r in t['training_results']:
    s=next(s for s in t['sessions'] if s['key']==r['session'])
    insert('training_results',session_id=sessions[r['session']],member_id=user[r['member']],coach_id=user[r['coach']],content=r['content'],attendance_status=r['attendance_status'],recorded_at=s['date']+' '+s['end'])
for r in t['training_plans']:insert('training_plans',coach_id=user[r['coach']],class_id=classes.get(r['sports_class']),member_id=user.get(r['member']),title=r['title'],content=r['content'],created_at=stamp(-10))
for r in t['evaluations']:insert('evaluations',member_id=user[r['member']],coach_id=user[r['coach']],evaluation_date=r['date'],progress_score=r['score'],comment=r['comment'])
for r in t['class_feedbacks']:insert('class_feedbacks',member_id=user[r['member']],class_id=classes[r['sports_class']],coach_id=user[course[r['sports_class']]['coach']],class_rating=r['rating'],coach_rating=r['rating'],comment=r['comment'],created_at=stamp(-1))
for r in t['center_incidents']:
    s=next(s for s in t['sessions'] if s['key']==r['session']);when=s['date']+' '+s['start']
    insert('center_incidents',id=1,session_id=sessions[r['session']],request_key=r['key'],started_at=when,ended_at=when,reason=r['reason'],evidence=r['evidence'],status=r['status'],created_by=user[r['created_by']],created_at=when,resolved_by=user['CM1'],resolved_at=when,recovered_credits=0,session_compensated=True)
for r in t['incident_affected_members']:insert('incident_affected_members',incident_id=1,member_id=user[r['member']])
for r in t['payment_resolutions']:
    state='NeedsReview' if r['status']=='TransferIssue' else r['status'];rid=resolutions[r['key']]
    inv=next(i for i in t['invoices'] if i['key']==r['invoice']);created=obligation_created(r['invoice'])
    insert('payment_resolutions',id=rid,invoice_id=invoices[r['invoice']],resolution_type=r['type'],status=state,amount=r['amount'],reason='DEMO '+r['origin'],created_by=user['CM1'],created_at=created.isoformat(),
           approved_by=user.get(r['approved_by']),approved_at=r.get('approved_at') or ((created+timedelta(minutes=1)).isoformat() if r['approved_by'] else None),completed_by=user.get(r['completed_by']),completed_at=stamp() if state=='Completed' else None,
           performed_at=stamp(-1) if state=='Completed' else None,evidence='DEMO private bank evidence retained' if state=='Completed' else None)
    if r['type']!='Refund':continue
    insert('normal_refunds',id=rid,class_id=classes[inv['sports_class']],origin='MemberChoice' if r['origin']=='MEMBER_REQUEST' else 'CenterCancellation',version=0,
           due_at=r.get('transfer_due_at'),first_due_at=r.get('transfer_due_at'),issue_note='DEMO unknown bank outcome; do not retry transfer' if state=='NeedsReview' else None)
    statements.append('UPDATE invoices SET resolution_status='+q('Completed' if state=='Completed' else 'RefundPending')+' WHERE invoice_id='+str(invoices[r['invoice']])+';')
    if r['receiver'] is None:continue
    receiver_id=rid
    verified=r.get('approved_by') is not None
    insert('normal_refund_receivers',id=receiver_id,refund_id=rid,bank='DEMO BANK',account_number='DEMOACCOUNT'+r['member'],account_name='DEMO '+r['member'],confirmed_by=user[r['member']],confirmed_at=r['receiver_confirmed_at'],
           verified_by=user['CM1'] if verified else None,verified_at=r['receiver_confirmed_at'] if verified else None,verification_source='DEMO Member and receiving account ownership verified' if verified else None)
    statements.append('UPDATE normal_refunds SET receiver_id='+str(receiver_id)+',approved_receiver_id='+str(receiver_id if verified else 'NULL')+' WHERE id='+str(rid)+';')
    attempt=next((x for x in t['refund_transfer_attempts'] if x['resolution']==r['key']),None)
    if attempt is None:continue
    aid=rid;when=attempt['performed_at'];verified_at=stamp() if state=='Completed' else None
    insert('normal_refund_attempts',id=aid,refund_id=rid,receiver_id=receiver_id,claim_key=str(uuid.uuid5(uuid.NAMESPACE_URL,'scms-demo/'+r['key'])),claimed_by=user['R1'],claimed_at=stamp(-2),status='Verified' if state=='Completed' else 'Submitted' if state=='AwaitingVerification' else 'Claimed',amount=r['amount'],currency='VND',bank_reference=attempt['bank_ref'],performed_at=when,verified_by=user['CM2'] if verified_at else None,verified_at=verified_at,bank_source='DEMO independent center bank statement' if verified_at else None,outcome_note='DEMO independently matched success' if verified_at else None)
    if state in ('Completed','AwaitingVerification'):
        from reportlab.pdfgen.canvas import Canvas
        buf=BytesIO();pdf=Canvas(buf,pagesize=(595,420),invariant=1);pdf.setTitle('DEMO refund evidence '+r['key'])
        pdf.setFont('Helvetica-Bold',28);pdf.drawString(36,365,'DEMO - NO REAL TRANSFER')
        pdf.setFont('Helvetica',13)
        for y,line in zip(range(315,100,-28),['SCMS manual Normal refund - synthetic test evidence','Case: '+r['key'],'Amount: '+str(r['amount'])+' VND','Reference: '+attempt['bank_ref'],'Performed: '+when,'Receiver: DEMO '+r['member'],'Manager must independently verify a center bank source.']):pdf.drawString(36,y,line)
        pdf.save();data=buf.getvalue()
        insert('normal_refund_documents',attempt_id=aid,uploaded_by=user['R1'],uploaded_at=when,content_type='application/pdf',sha256=hashlib.sha256(data).hexdigest(),content=raw("decode('"+data.hex()+"','hex')"))
        if a.preview_dir:
            preview=Path(a.preview_dir);preview.mkdir(parents=True,exist_ok=True);(preview/(r['key']+'.pdf')).write_bytes(data)
        assert b'%PDF-' in data and len(data)<5*1024*1024
insert('normal_refund_verifications',attempt_id=resolutions['RF06'],verified_by=user['CM2'],verified_at=stamp(),outcome='Success',bank_source='DEMO independent center bank statement',note='DEMO receipt and receiver matched independently')
# Actual consent snapshots. M01 is WAITING: accepting a transfer must commit the right, not just a label.
for key,pid,kind,status,shift,target in [('N05',1,'Transfer','Pending',None,'N11'),('N04',2,'Postpone','Applied',7,None)]:
    c=course[key]
    def fingerprint():
        result=''
        for ck in sorted([key]+([target] if target else []),key=lambda x:classes[x]):
            x=course[ck];result+=f"{classes[ck]}:{x['status']}:{x['price']:.2f}:{x['max_capacity']}:{x['min_capacity']}:{user[x['coach']]}"
            for s in sorted([v for v in t['sessions'] if v['sports_class']==ck and v['status']!='Cancelled' and not v['center_invalidated']],key=lambda v:(v['date'],v['start'])):
                # room IDs are catalog-owned and stable in this rebuild (resolved below with SQL digest).
                result+=f"|{sessions[s['key']]}:{s['date']}:{s['start']}:{s['end']}:ROOM({s['room']}):{user[s['coach']]}:{s['status']}"
        return result
    insert('normal_proposals',id=pid,class_id=classes[key],target_class_id=classes.get(target),kind=kind,status=status,shift_days=shift,version=1,fingerprint='RECOMPUTE_AFTER_FIXTURE',schedule_json='[]',reason='DEMO center cannot provide original class' if key=='N05' else 'DEMO insufficient enrollment; one postponement applied',created_by=user['CM1'],created_at=stamp(-3,'00:00'))
    for e in t['class_enrollments']:
        if e['sports_class']!=key:continue
        insert('normal_responses',proposal_id=pid,invoice_id=invoices[e['invoice']],member_id=user[e['member']],choice='Refund' if key=='N05' and e['member']=='M02' else 'Waiting' if key=='N05' else 'Accept',version=1,responded_at=stamp(-3,'00:00'))
    statements.append("UPDATE normal_proposals p SET schedule_json=(SELECT json_agg(json_build_object('sessionId',s.session_id,'date',s.session_date,'start',to_char(s.start_time,'HH24:MI'),'end',to_char(s.end_time,'HH24:MI'),'coachId',s.coach_id,'roomId',s.room_id) ORDER BY s.session_date,s.start_time)::text FROM sessions s WHERE s.class_id=coalesce(p.target_class_id,p.class_id) AND s.status<>'Cancelled' AND NOT s.center_invalidated) WHERE p.id="+str(pid)+";")
    # Same order/format as NormalChangeService, generated from the rows actually inserted.
    statements.append("UPDATE normal_proposals p SET fingerprint=encode(extensions.digest((SELECT string_agg(piece,'' ORDER BY class_id) FROM (SELECT c.class_id,c.class_id||':'||c.status||':'||c.tuition_fee||':'||c.max_capacity||':'||c.min_capacity||':'||c.coach_id||coalesce((SELECT string_agg('|'||s.session_id||':'||s.session_date||':'||to_char(s.start_time,'HH24:MI')||':'||to_char(s.end_time,'HH24:MI')||':'||s.room_id||':'||s.coach_id||':'||s.status,'' ORDER BY s.session_date,s.start_time) FROM sessions s WHERE s.class_id=c.class_id AND s.status<>'Cancelled' AND NOT s.center_invalidated),'') AS piece FROM classes c WHERE c.class_id=p.class_id OR c.class_id=p.target_class_id) x),'sha256'),'hex') WHERE p.id="+str(pid)+";")
# Private DEMO notifications/audit refer to real IDs. No banking details or public object URLs.
for idx,r in enumerate(t['notifications'],1):
    insert('notifications',notification_id=idx,user_id=user[r['user']],title='DEMO workflow update',content=r['content'],type='System',notification_type='PAYMENT_RECONCILED',related_type='CLASS',related_id=classes['N05'] if idx==1 else classes['N06'] if idx==2 else classes['N12'],dedupe_key='DEMO:'+r['key'],is_read=r['is_read'],created_at=stamp())
    insert('notification_recipients',notification_id=idx,user_id=user[r['user']],is_read=r['is_read'],created_at=stamp())
for r in t['system_logs']:
    if r['actor']=='ADM':continue
    insert('system_logs',user_id=user[r['actor']],action=r['action'],target_entity='CLASS',target_id=classes['N05'] if r['target']=='N05' else classes['N06'] if r['target']=='N06' else classes['N12'],detail='DEMO fixture: synthetic history, not observed provider operations',timestamp=stamp(),audit_category='BUSINESS',center_id=1,scope_source='TARGET_AT_EVENT')
insert('chat_conversations',id=1,member_user_id=user['M08'],assigned_staff_user_id=user['R1'],status='OPEN',handler_type='STAFF',subject='DEMO refund inquiry',created_at=stamp(),updated_at=stamp(),last_message_at=stamp())
for mid,r in enumerate(t['chat_messages'],1):
    insert('chat_messages',id=mid,conversation_id=1,sender_type='MEMBER' if r['sender']=='M08' else 'STAFF',sender_user_id=user[r['sender']],content=r['content'],content_type='TEXT',client_message_id='DEMO-MSG-'+str(mid),created_at=stamp())
insert('chat_read_state',conversation_id=1,user_id=user['M08'],last_read_message_id=1,updated_at=stamp())
insert('chat_read_state',conversation_id=1,user_id=user['R1'],last_read_message_id=2,updated_at=stamp())
statements += ['ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;','ALTER TABLE sessions ENABLE TRIGGER trg_sessions_validate;']
output=(ROOT/a.output).resolve();assert output.is_relative_to(ROOT)
output.write_text('\n'.join(statements)+'\n',encoding='utf-8')
print(json.dumps({'output':str(output.relative_to(ROOT)),'anchor':str(anchor),'users':47,'classes':21,'privateDemoPdfs':2,'databaseAccess':False}))
