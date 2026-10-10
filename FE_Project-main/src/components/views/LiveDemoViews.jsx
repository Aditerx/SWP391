import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useSCMS } from '../../context/SCMSContext';
import { demoApi as api } from '../../api/demoApi';
import { getErrorMessage } from '../../api/apiErrors';

const money = (value) => Number(value || 0).toLocaleString('vi-VN') + ' đ';
const panel = 'rounded-xl border border-stone-200 bg-white p-5 shadow-sm';
const input = 'rounded-lg border border-stone-300 p-2 text-stone-900 bg-white';
const button = 'rounded-lg bg-[#BA0000] px-4 py-2 text-white disabled:opacity-50';
const secondary = 'rounded-lg border border-stone-300 px-3 py-2 disabled:opacity-50';
const statusNames = { Pending: 'Chờ thanh toán', Paid: 'Đã thanh toán', Active: 'Đã kích hoạt', Registered: 'Đã ghi danh', Cancelled: 'Đã hủy', Expired: 'Hết hạn', Completed: 'Hoàn thành', Present: 'Có mặt', Absent: 'Vắng', Late: 'Đi muộn', Open: 'Mở đăng ký', Ongoing: 'Đang học', Closed: 'Đã đóng' };
const status = (value) => statusNames[value] || value;
const dateTime = (value) => value ? value.replace('T', ' ').slice(0, 19) : '—';

function useAction() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const locked = useRef(false);
  const run = async (task) => {
    if (locked.current) return;
    locked.current = true; setBusy(true); setMessage(''); setError('');
    try { await task(setMessage); }
    catch (e) { setError(getErrorMessage(e, 'Không thể hoàn tất thao tác.')); }
    finally { locked.current = false; setBusy(false); }
  };
  return { busy, message, error, run };
}

function Notice({ error, message, loading }) {
  return <>{loading && <p role="status">Đang tải dữ liệu…</p>}{error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-700">{error}</p>}{message && <p role="status" className="rounded-lg bg-green-50 p-3 text-green-700">{message}</p>}</>;
}

function InvoiceTable({ invoices, cashier = false, onConfirm, onCancel, busy }) {
  return <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Mã / Hội viên</th><th>Nội dung</th><th>Số tiền</th><th>Trạng thái / hạn thu</th><th>Thao tác</th></tr></thead><tbody>
    {invoices.map(i => <tr key={i.invoiceId} className="border-b"><td className="p-2">{i.invoiceCode || `#${i.invoiceId}`}<br />{i.memberName}<br /><span className="text-stone-500">{i.memberEmail}</span></td><td>{i.className ? `Học phí: ${i.className}` : `Gói: ${i.packageName}`}<br />{i.paymentMethod || '—'}</td><td>{money(i.amount)}</td><td>{status(i.paymentStatus)}<br />{dateTime(i.expiresAt)}</td><td><div className="flex gap-2">{i.paymentStatus === 'Pending' && <>
      {cashier && i.paymentMethod === 'Cash' && <button disabled={busy} className={button} onClick={() => onConfirm(i)}>Xác nhận đã thu Cash</button>}
      {onCancel && <button disabled={busy} className={secondary} onClick={() => onCancel(i)}>Hủy yêu cầu</button>}
    </>}</div></td></tr>)}
    {!invoices.length && <tr><td className="p-3" colSpan={5}>Chưa có hóa đơn.</td></tr>}
  </tbody></table></div>;
}

export function LiveMembershipView() {
  const [data, setData] = useState({ packages: [], subscriptions: [], invoices: [], member: null });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const action = useAction();
  const refresh = useCallback(async () => {
    setLoading(true); setLoadError('');
    try {
      const member = await api.me();
      const memberId = member.id ?? member.memberId;
      if (!Number.isInteger(memberId) || memberId < 1) throw new Error('Backend chưa trả ID hội viên hợp lệ.');
      const [packages, subscriptions, invoices] = await Promise.all([api.packages(), api.subscriptions(memberId), api.invoices(true)]);
      setData({ member, packages, subscriptions, invoices });
    } catch (e) { setLoadError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  const buy = (pkg) => action.run(async (notify) => {
    const order = await api.subscribe(pkg.id);
    notify(`Yêu cầu ${order.invoiceCode} đã tạo: ${money(order.amount)}. Mang mã đến lễ tân đóng tiền mặt. Gói sẽ được kích hoạt sau khi thu tiền.`);
    await refresh();
  });
  return <div className="space-y-5 text-stone-900"><div className="flex justify-between"><h2 className="text-xl font-bold">Gói thành viên và hóa đơn</h2><button className={secondary} disabled={loading || action.busy} onClick={refresh}>Tải lại</button></div>
    <Notice error={loadError || action.error} message={action.message} loading={loading} />
    {data.member && <p>{data.member.fullName || data.member.name} · {data.member.email} · <Link className="text-red-700 underline" to="/member/catalog">Đăng ký lớp học</Link></p>}
    <div className="grid gap-4 md:grid-cols-3">{data.packages.filter(p => p.status === 'Active').map(p => <div className={panel} key={p.id}><h3 className="font-bold">{p.name}</h3><p>{p.durationDays} ngày · {money(p.price)}</p><p className="my-2">{p.benefits}</p><button className={button} disabled={action.busy || loading} onClick={() => buy(p)}>Đăng ký gói · Cash</button></div>)}</div>
    <section className={panel}><h3 className="mb-3 font-bold">Các gói đã đăng ký</h3>{data.subscriptions.map(s => <p className="border-b py-2" key={s.subscriptionId}>{s.packageName} · {status(s.status)} · {s.startDate} → {s.endDate}</p>)}{!data.subscriptions.length && <p>Chưa đăng ký gói.</p>}</section>
    <section className={panel}><h3 className="mb-3 font-bold">Hóa đơn của tôi</h3><InvoiceTable invoices={data.invoices} busy={action.busy} onCancel={i => action.run(async notify => { await api.cancelInvoice(i.invoiceId); notify('Đã hủy yêu cầu thanh toán.'); await refresh(); })} /></section>
  </div>;
}

export function LiveCashPaymentsView({ readOnly = false }) {
  const [invoices, setInvoices] = useState([]);
  const [members, setMembers] = useState([]);
  const [packages, setPackages] = useState([]);
  const [memberId, setMemberId] = useState('');
  const [packageId, setPackageId] = useState('');
  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState('');
  const [confirming, setConfirming] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const action = useAction();
  const refresh = useCallback(async () => {
    setLoading(true); setError('');
    try { setInvoices(await api.invoices(false, { keyword, status: filter })); }
    catch (e) { setError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, [keyword, filter]);
  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => { if (!readOnly) Promise.all([api.members(), api.packages()]).then(([m, p]) => { setMembers(m); setPackages(p); }).catch(e => setError(getErrorMessage(e))); }, [readOnly]);
  return <div className="space-y-5 text-stone-900"><h2 className="text-xl font-bold">{readOnly ? 'Tra cứu hóa đơn' : 'Thu tiền mặt'}</h2>
    <Notice error={error || action.error} message={action.message} loading={loading} />
    {!readOnly && <form className={`${panel} flex flex-wrap items-end gap-3`} onSubmit={e => { e.preventDefault(); action.run(async notify => { const order = await api.subscribe(packageId, Number(memberId)); notify(`Đã tạo ${order.invoiceCode}, số tiền ${money(order.amount)}. Chọn hóa đơn và xác nhận khi đã nhận tiền.`); await refresh(); }); }}>
      <label>Hội viên<br /><select className={input} required value={memberId} onChange={e => setMemberId(e.target.value)}><option value="">Chọn hội viên</option>{members.map(m => <option key={m.id} value={m.id}>{m.name} · {m.email}</option>)}</select></label>
      <label>Gói thành viên<br /><select className={input} required value={packageId} onChange={e => setPackageId(e.target.value)}><option value="">Chọn gói</option>{packages.filter(p => p.status === 'Active').map(p => <option key={p.id} value={p.id}>{p.name} · {money(p.price)}</option>)}</select></label>
      <button className={button} disabled={action.busy}>Tạo yêu cầu Cash tại quầy</button>
    </form>}
    <div className="flex flex-wrap gap-3"><input aria-label="Tìm hóa đơn" className={input} placeholder="Mã hóa đơn, tên hội viên, gói hoặc lớp" value={keyword} onChange={e => setKeyword(e.target.value)} /><select aria-label="Trạng thái hóa đơn" className={input} value={filter} onChange={e => setFilter(e.target.value)}><option value="">Tất cả</option>{['Pending', 'Paid', 'Cancelled', 'Expired'].map(s => <option key={s} value={s}>{status(s)}</option>)}</select><button className={secondary} disabled={loading || action.busy} onClick={refresh}>Tải lại</button></div>
    <section className={panel}><InvoiceTable invoices={invoices} cashier={!readOnly} busy={action.busy || loading} onConfirm={setConfirming} onCancel={!readOnly ? i => action.run(async notify => { await api.cancelInvoice(i.invoiceId); notify('Đã hủy hóa đơn và yêu cầu liên quan.'); await refresh(); }) : null} /></section>
    {confirming && <div role="dialog" aria-modal="true" aria-label="Xác nhận thu tiền mặt" className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"><div className={`${panel} max-w-lg space-y-4`}><h3 className="font-bold">Xác nhận đã nhận tiền mặt</h3><p>{confirming.invoiceCode} · {confirming.memberName}</p><p>{confirming.packageName || confirming.className} · <strong>{money(confirming.amount)}</strong></p><Notice error={action.error} /><div className="flex gap-3"><button className={button} disabled={action.busy} onClick={() => action.run(async notify => { await api.confirmCash(confirming.invoiceId); setConfirming(null); notify('Đã thu Cash. Hóa đơn đã thanh toán, gói/lớp được kích hoạt.'); await refresh(); })}>Đã nhận đủ tiền · Xác nhận</button><button className={secondary} disabled={action.busy} onClick={() => setConfirming(null)}>Quay lại</button></div></div></div>}
  </div>;
}

function MemberFeedback({ sportsClass }) {
  const [mine, setMine] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [classRating, setClassRating] = useState(5);
  const [coachRating, setCoachRating] = useState(5);
  const [comment, setComment] = useState('');
  const action = useAction();
  useEffect(() => { api.feedbackMine(sportsClass.id).then(setMine).catch(e => { if (e.status !== 404) setLoadError(getErrorMessage(e)); }).finally(() => setLoading(false)); }, [sportsClass.id]);
  if (mine) return <p className="mt-3 text-green-700">Đã gửi feedback · lớp {mine.classRating}/5 · mentor {mine.coachRating}/5 · {mine.comment}</p>;
  return <form className="mt-4 space-y-2 border-t pt-3" onSubmit={e => { e.preventDefault(); action.run(async () => { setMine(await api.feedback(sportsClass.id, { coachId: sportsClass.coachId, classRating: Number(classRating), coachRating: Number(coachRating), comment })); }); }}>
    <p className="text-sm">Feedback lớp và mentor sau khi có ít nhất một buổi Có mặt (Present).</p><Notice error={loadError || action.error} loading={loading} />
    <div className="flex flex-wrap gap-3">{[['Điểm lớp', classRating, setClassRating], ['Điểm mentor', coachRating, setCoachRating]].map(([name, value, setter]) => <label key={name}>{name} <select className={input} value={value} onChange={e => setter(e.target.value)}>{[1, 2, 3, 4, 5].map(n => <option key={n} value={n}>{n}/5</option>)}</select></label>)}</div>
    <textarea className={`${input} w-full`} aria-label="Nội dung feedback" maxLength={1000} value={comment} onChange={e => setComment(e.target.value)} placeholder="Nhận xét lớp và mentor" /><button className={button} disabled={loading || !!loadError || action.busy}>Gửi feedback</button>
  </form>;
}

export function LiveClassCatalog({ receptionist = false }) {
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [members, setMembers] = useState([]);
  const [memberId, setMemberId] = useState('');
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const action = useAction();
  const refresh = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [c, s, e] = await Promise.all([api.classes(), api.subjects(), receptionist && !memberId ? Promise.resolve([]) : api.enrollments(receptionist ? { memberId } : {})]);
      setClasses(c); setSubjects(s); setEnrollments(e);
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, [receptionist, memberId]);
  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => { if (receptionist) api.members().then(setMembers).catch(e => setError(getErrorMessage(e))); }, [receptionist]);
  return <div className="space-y-5 text-stone-900"><div className="flex justify-between"><h2 className="text-xl font-bold">Bộ môn · lớp học</h2><button className={secondary} disabled={loading || action.busy} onClick={refresh}>Tải lại</button></div><Notice error={error || action.error} message={action.message} loading={loading} />
    <div className="flex gap-3 flex-wrap">{receptionist && <select aria-label="Hội viên ghi danh" className={input} value={memberId} onChange={e => setMemberId(e.target.value)}><option value="">Chọn hội viên</option>{members.map(m => <option key={m.id} value={m.id}>{m.name} · {m.email}</option>)}</select>}<select aria-label="Lọc bộ môn" className={input} value={subjectId} onChange={e => setSubjectId(e.target.value)}><option value="">Tất cả bộ môn</option>{subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select>{!receptionist && <Link className="text-red-700 underline" to="/member/my-membership">Gói thành viên / hóa đơn</Link>}</div>
    <div className="grid gap-4 lg:grid-cols-2">{classes.filter(c => (!subjectId || String(c.subjectId) === subjectId) && (['Open', 'Ongoing'].includes(c.status) || enrollments.some(e => e.classId === c.id))).map(c => {
      const enrollment = enrollments.find(e => e.classId === c.id && ['Pending', 'Registered', 'Completed'].includes(e.status));
      return <section key={c.id} className={panel}><h3 className="font-bold">{c.name}</h3><p>{subjects.find(s => s.id === c.subjectId)?.name} · {status(c.status)}</p><p>{c.startDate} → {c.endDate} · {c.enrolledCount}/{c.maxCapacity} chỗ</p><p className="my-2">Học phí cả lớp: {money(c.tuitionFee)}</p>
        {enrollment ? <div><p>{status(enrollment.status)}</p>{enrollment.status !== 'Completed' && <button className={secondary} disabled={action.busy || loading} onClick={() => action.run(async notify => { await api.cancelEnrollment(enrollment.id); notify('Đã hủy ghi danh theo quy định của lớp.'); await refresh(); })}>Hủy ghi danh</button>}</div> : <button className={button} disabled={action.busy || loading || (receptionist && !memberId) || c.enrolledCount >= c.maxCapacity || !['Open', 'Ongoing'].includes(c.status)} onClick={() => action.run(async notify => { const result = await api.enroll(c.id, receptionist ? memberId : null); notify(result.status === 'Pending' ? `Đã giữ chỗ. Hóa đơn ${result.invoiceCode}: mang mã đến lễ tân đóng học phí.` : 'Đã ghi danh lớp miễn phí.'); await refresh(); })}>Đăng ký lớp · Cash</button>}
        {!receptionist && enrollment && ['Registered', 'Completed'].includes(enrollment.status) && ['Ongoing', 'Closed'].includes(c.status) && <MemberFeedback sportsClass={c} />}
      </section>;
    })}</div>
  </div>;
}

function RosterMember({ enrollment, existing, session, afterSave }) {
  const [state, setState] = useState(existing?.attendanceStatus || 'Present');
  const [content, setContent] = useState(existing?.content || '');
  const [reason, setReason] = useState('');
  const action = useAction();
  return <form className="border-b py-3 space-y-2" onSubmit={e => { e.preventDefault(); action.run(async notify => { await api.record({ sessionId: session.id, memberId: enrollment.memberId, coachId: session.coachId, attendanceStatus: state, content, reason }); notify('Đã lưu điểm danh và nhận xét.'); await afterSave(); }); }}>
    <p className="font-semibold">{enrollment.memberName} · {enrollment.memberEmail} · {status(enrollment.status)}</p><Notice error={action.error} message={action.message} />
    <div className="flex flex-wrap gap-2"><select className={input} aria-label={`Điểm danh ${enrollment.memberName}`} value={state} onChange={e => setState(e.target.value)}>{['Present', 'Absent', 'Late'].map(s => <option key={s} value={s}>{status(s)}</option>)}</select><input className={input} aria-label="Nhận xét buổi học" placeholder="Nhận xét buổi học" value={content} onChange={e => setContent(e.target.value)} /><input className={input} aria-label="Lý do sửa điểm danh" placeholder="Lý do sửa điểm danh quá khứ" value={reason} onChange={e => setReason(e.target.value)} /><button className={button} disabled={action.busy}>Lưu điểm danh</button></div>
  </form>;
}

export function LiveAttendanceView({ member = false }) {
  const { currentUser } = useSCMS();
  const [sessions, setSessions] = useState([]);
  const [sessionId, setSessionId] = useState('');
  const [roster, setRoster] = useState([]);
  const [results, setResults] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const refresh = useCallback(async () => {
    setLoading(true); setError('');
    try {
      if (member) {
        const me = await api.me();
        const [s, a] = await Promise.all([api.sessions({ memberId: me.id ?? me.memberId }), api.attendance({})]);
        setSessions(s); setAttendance(a);
      } else {
        const [s, f] = await Promise.all([api.sessions({ coachId: currentUser.id }), api.coachFeedback()]);
        setSessions(s); setFeedback(f);
      }
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, [member, currentUser.id]);
  useEffect(() => { refresh(); }, [refresh]);
  const session = sessions.find(s => String(s.id) === sessionId);
  const refreshRoster = useCallback(async () => {
    if (!session || member) return;
    setError(''); setRoster([]); setResults([]);
    try { const [r, t] = await Promise.all([api.roster(session.classId), api.results({ sessionId: session.id })]); setRoster(r); setResults(t); }
    catch (e) { setError(getErrorMessage(e)); }
  }, [session, member]);
  useEffect(() => { refreshRoster(); }, [refreshRoster]);
  return <div className="space-y-5 text-stone-900"><div className="flex justify-between"><h2 className="text-xl font-bold">{member ? 'Lịch học và điểm danh của tôi' : 'Lịch lớp · điểm danh · nhận xét'}</h2><button className={secondary} disabled={loading} onClick={async () => { await refresh(); await refreshRoster(); }}>Tải lại</button></div><Notice error={error} loading={loading} />
    <section className={panel}><select aria-label="Chọn buổi học" className={`${input} max-w-full`} value={sessionId} onChange={e => setSessionId(e.target.value)}><option value="">Chọn buổi học</option>{sessions.map(s => <option key={s.id} value={s.id}>{s.sessionDate} · {s.startTime}–{s.endTime} · {s.className} · {s.roomName}</option>)}</select>
      {session && <p className="my-3">{session.className} · {session.coachName} · {session.roomName} · {status(session.status)}</p>}
      {!member && session && <>{roster.filter(e => ['Registered', 'Completed'].includes(e.status)).map(e => <RosterMember key={`${session.id}-${e.id}-${results.find(r => r.memberId === e.memberId)?.recordedAt || ''}`} enrollment={e} existing={results.find(r => r.memberId === e.memberId)} session={session} afterSave={refreshRoster} />)}<p className="mt-3 text-sm text-stone-500">Chỉ hội viên đã ghi danh/hoàn thành được điểm danh. Các chỗ chờ đóng học phí không xuất hiện trong danh sách điểm danh.</p></>}
      {member && attendance.filter(a => !sessionId || String(a.sessionId) === sessionId).map(a => <p className="border-b py-3" key={a.attendanceId}>{a.sessionDate} · {a.className} · {status(a.state)} · {dateTime(a.checkInTime)}</p>)}
    </section>
    {!member && feedback && <section className={panel}><h3 className="font-bold">Feedback dành cho mentor</h3><p>{feedback.totalFeedback} đánh giá · Trung bình {feedback.averageCoachRating ?? '—'}/5</p>{feedback.feedback.map((f, n) => <p className="border-b py-3" key={f.feedbackId ?? n}>{f.className} · {f.coachRating}/5 · {f.comment}</p>)}</section>}
  </div>;
}

export function LiveManagerFeedback() {
  const [classes, setClasses] = useState([]);
  const [classId, setClassId] = useState('');
  const [feedback, setFeedback] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api.classes().then(setClasses).catch(e => setError(getErrorMessage(e))); }, []);
  useEffect(() => { setFeedback(null); setError(''); if (classId) api.classFeedback(classId).then(setFeedback).catch(e => setError(getErrorMessage(e))); }, [classId]);
  return <section className={`${panel} my-5 text-stone-900`}><h3 className="mb-3 font-bold">Feedback lớp và mentor</h3><Notice error={error} /><select className={input} aria-label="Lớp xem feedback" value={classId} onChange={e => setClassId(e.target.value)}><option value="">Chọn lớp xem feedback</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>{feedback && <div className="mt-3"><p>{feedback.totalFeedback} đánh giá · Điểm lớp {feedback.averageClassRating ?? '—'}/5 · Điểm mentor {feedback.averageCoachRating ?? '—'}/5</p>{feedback.feedback.map((f, n) => <p className="border-b py-3" key={f.feedbackId ?? n}>{f.memberName} · {f.classRating}/5 · {f.coachRating}/5 · {f.comment}</p>)}</div>}</section>;
}

export function LiveManagerSessions() {
  const [classes, setClasses] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [classId, setClassId] = useState('');
  const [roomId, setRoomId] = useState('');
  const [date, setDate] = useState(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date()));
  const [start, setStart] = useState('08:00');
  const [end, setEnd] = useState('09:00');
  const [error, setError] = useState('');
  const action = useAction();
  const refresh = useCallback(async () => {
    setError('');
    try { const [c, r, s] = await Promise.all([api.classes(), api.rooms(), api.sessions()]); setClasses(c); setRooms(r); setSessions(s); }
    catch (e) { setError(getErrorMessage(e)); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  return <section className={`${panel} my-5 text-stone-900`}><div className="flex justify-between"><h3 className="font-bold">Buổi học của lớp</h3><button className={secondary} disabled={action.busy} onClick={refresh}>Tải lại lớp và lịch</button></div><Notice error={error || action.error} message={action.message} />
    <form className="my-4 flex flex-wrap items-end gap-3" onSubmit={e => { e.preventDefault(); action.run(async notify => { await api.createSession({ classId: Number(classId), roomId: Number(roomId), sessionDate: date, startTime: start, endTime: end, status: 'Scheduled' }); notify('Đã tạo buổi học. HLV và hội viên ghi danh có thể xem lịch.'); await refresh(); }); }}>
      <label>Lớp<br /><select required className={input} value={classId} onChange={e => setClassId(e.target.value)}><option value="">Chọn lớp</option>{classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <label>Phòng<br /><select required className={input} value={roomId} onChange={e => setRoomId(e.target.value)}><option value="">Chọn phòng</option>{rooms.filter(r => r.status === 'Available').map(r => <option key={r.id} value={r.id}>{r.name} · {r.capacity} chỗ</option>)}</select></label>
      <label>Ngày<br /><input className={input} type="date" required value={date} onChange={e => setDate(e.target.value)} /></label><label>Bắt đầu<br /><input className={input} type="time" required value={start} onChange={e => setStart(e.target.value)} /></label><label>Kết thúc<br /><input className={input} type="time" required value={end} onChange={e => setEnd(e.target.value)} /></label><button className={button} disabled={action.busy}>Tạo buổi học</button>
    </form>{sessions.filter(s => !classId || s.classId === Number(classId)).map(s => <p className="border-b py-2 text-sm" key={s.id}>{s.sessionDate} · {s.startTime}–{s.endTime} · {s.className} · {s.coachName} · {s.roomName} · {status(s.status)}</p>)}
  </section>;
}

export function LiveManagerClasses() {
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [coaches, setCoaches] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const action = useAction();
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());
  const empty = () => ({ name: '', subjectId: '', coachId: '', maxCapacity: 10, tuitionFee: 0, startDate: today, endDate: today, status: 'Open' });
  const [form, setForm] = useState(empty);
  const refresh = useCallback(async () => {
    setLoading(true); setError('');
    try { const [c, s, t] = await Promise.all([api.classes(), api.subjects(), api.staff()]); setClasses(c); setSubjects(s); setCoaches(t.filter(u => u.role === 'coach' && u.status === 'active')); }
    catch (e) { setError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  const field = (key) => ({ value: form[key] ?? '', onChange: e => setForm(f => ({ ...f, [key]: e.target.value })) });
  return <div className="space-y-5 text-stone-900"><div className="flex justify-between"><h2 className="text-xl font-bold">Quản lý lớp học</h2><button className={secondary} disabled={action.busy || loading} onClick={refresh}>Tải lại</button></div><Notice error={error || action.error} message={action.message} loading={loading} />
    <form className={`${panel} space-y-3`} onSubmit={e => { e.preventDefault(); action.run(async notify => { await api.saveClass({ ...form, subjectId: Number(form.subjectId), coachId: form.coachId ? Number(form.coachId) : null, maxCapacity: Number(form.maxCapacity), tuitionFee: Number(form.tuitionFee) }, editing); notify(editing ? 'Đã cập nhật lớp.' : 'Đã tạo lớp. Tiếp tục tạo buổi học bên dưới.'); setEditing(null); setForm(empty()); await refresh(); }); }}>
      <h3 className="font-bold">{editing ? `Sửa lớp #${editing}` : 'Tạo lớp học'}</h3><div className="flex flex-wrap gap-3">
        <label>Tên lớp<br /><input className={input} required {...field('name')} /></label><label>Bộ môn<br /><select className={input} required {...field('subjectId')}><option value="">Chọn bộ môn</option>{subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label><label>Mentor / HLV<br /><select className={input} {...field('coachId')}><option value="">Chưa phân công</option>{coaches.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
        <label>Số chỗ tối đa<br /><input className={input} type="number" min="1" required {...field('maxCapacity')} /></label><label>Học phí cả lớp (đ)<br /><input className={input} type="number" min="0" step="1" required {...field('tuitionFee')} /></label><label>Ngày bắt đầu<br /><input className={input} type="date" required {...field('startDate')} /></label><label>Ngày kết thúc<br /><input className={input} type="date" min={form.startDate} required {...field('endDate')} /></label><label>Trạng thái<br /><select className={input} {...field('status')}>{['Open', 'Ongoing', 'Closed', 'Cancelled'].map(s => <option key={s} value={s}>{status(s)}</option>)}</select></label>
      </div><div className="flex gap-3"><button className={button} disabled={loading || action.busy}>{editing ? 'Lưu thay đổi' : 'Tạo lớp'}</button>{editing && <button type="button" className={secondary} onClick={() => { setEditing(null); setForm(empty()); }}>Bỏ sửa</button>}</div>
    </form><div className="grid gap-4 md:grid-cols-2">{classes.map(c => <section className={panel} key={c.id}><h3 className="font-bold">{c.name}</h3><p>{subjects.find(s => s.id === c.subjectId)?.name} · {coaches.find(s => s.id === c.coachId)?.name || 'Chưa phân công'}</p><p>{c.startDate} → {c.endDate} · {status(c.status)}</p><p>{c.enrolledCount}/{c.maxCapacity} chỗ · Học phí cả lớp {money(c.tuitionFee)}</p><button className={`${secondary} mt-3`} disabled={action.busy} onClick={() => { setEditing(c.id); setForm({ ...c }); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Sửa lớp</button></section>)}</div>
  </div>;
}
