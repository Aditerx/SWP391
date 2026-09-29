import React, { useState } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { formatDate } from '../../locales/translations';
import { StatusBadge } from '../common/Badge';
import {
  Calendar,
  Clock,
  MapPin,
  Plus,
  Trash2,
  CheckCircle2,
  AlertTriangle,
  X,
  Repeat,
  Sparkles,
  ShieldAlert
} from 'lucide-react';

export function ClassScheduleModal({ sportsClass, isOpen, onClose }) {
  const {
    rooms,
    sessions,
    createSession,
    generateSessions,
    updateSessionStatus,
    deleteSession,
    checkConflict,
    language,
    t
  } = useSCMS();

  const [activeTab, setActiveTab] = useState('list'); // 'list' | 'create' | 'generate'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [conflictWarning, setConflictWarning] = useState(null);

  // Single Session Form
  const [singleForm, setSingleForm] = useState({
    sessionDate: sportsClass?.startDate || new Date().toISOString().split('T')[0],
    startTime: '07:00',
    endTime: '08:00',
    roomId: rooms[0]?.id || 1,
  });

  // Batch Generation Form
  const [batchForm, setBatchForm] = useState({
    startDate: sportsClass?.startDate || new Date().toISOString().split('T')[0],
    endDate: sportsClass?.endDate || new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
    daysOfWeek: [1, 3, 5], // Mon, Wed, Fri
    startTime: '07:00',
    endTime: '08:00',
    roomId: rooms[0]?.id || 1,
  });

  if (!isOpen || !sportsClass) return null;

  const classSessions = sessions.filter(s => String(s.classId) === String(sportsClass.id));

  const handleCheckConflict = async () => {
    setError('');
    setSuccess('');
    try {
      const res = await checkConflict({
        classId: sportsClass.id,
        roomId: singleForm.roomId,
        sessionDate: singleForm.sessionDate,
        startTime: singleForm.startTime,
        endTime: singleForm.endTime
      });
      setConflictWarning(res);
      if (!res.hasConflict) {
        setSuccess(language === 'vi' ? 'Không có xung đột! Khung giờ và phòng hoàn toàn sẵn sàng.' : 'No conflict detected! Time slot and room are available.');
      }
    } catch (err) {
      setError(err?.message || String(err));
    }
  };

  const handleCreateSingle = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    setConflictWarning(null);
    try {
      await createSession({
        classId: sportsClass.id,
        roomId: singleForm.roomId,
        sessionDate: singleForm.sessionDate,
        startTime: singleForm.startTime,
        endTime: singleForm.endTime
      });
      setSuccess(language === 'vi' ? 'Đã thêm buổi học thành công!' : 'Session created successfully!');
      setActiveTab('list');
    } catch (err) {
      setError(err?.message || String(err));
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateBatch = async (e) => {
    e.preventDefault();
    if (!batchForm.daysOfWeek.length) {
      setError(language === 'vi' ? 'Vui lòng chọn ít nhất một thứ trong tuần' : 'Please select at least one day of the week');
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await generateSessions({
        classId: sportsClass.id,
        roomId: batchForm.roomId,
        startDate: batchForm.startDate,
        endDate: batchForm.endDate,
        daysOfWeek: batchForm.daysOfWeek,
        startTime: batchForm.startTime,
        endTime: batchForm.endTime
      });
      setSuccess(language === 'vi' ? 'Đã tự động tạo toàn bộ lịch học cho lớp!' : 'Successfully generated class schedule!');
      setActiveTab('list');
    } catch (err) {
      setError(err?.message || String(err));
    } finally {
      setLoading(false);
    }
  };

  const toggleDayOfWeek = (day) => {
    setBatchForm(prev => {
      const exists = prev.daysOfWeek.includes(day);
      return {
        ...prev,
        daysOfWeek: exists
          ? prev.daysOfWeek.filter(d => d !== day)
          : [...prev.daysOfWeek, day].sort()
      };
    });
  };

  const dayLabels = [
    { num: 1, label: language === 'vi' ? 'Thứ 2' : 'Mon' },
    { num: 2, label: language === 'vi' ? 'Thứ 3' : 'Tue' },
    { num: 3, label: language === 'vi' ? 'Thứ 4' : 'Wed' },
    { num: 4, label: language === 'vi' ? 'Thứ 5' : 'Thu' },
    { num: 5, label: language === 'vi' ? 'Thứ 6' : 'Fri' },
    { num: 6, label: language === 'vi' ? 'Thứ 7' : 'Sat' },
    { num: 7, label: language === 'vi' ? 'Chủ nhật' : 'Sun' },
  ];

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-2xl rounded-xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600/30 rounded-lg border border-blue-400/30 text-blue-300">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold tracking-tight">
                {language === 'vi' ? 'Lịch Học & Buổi Tập' : 'Class Sessions & Schedule'} — {sportsClass.nameVi || sportsClass.name}
              </h3>
              <p className="text-xs text-slate-300 mt-0.5">
                {language === 'vi' ? 'HLV:' : 'Coach:'} <span className="font-semibold text-white">{sportsClass.coachName || 'Chưa phân công'}</span> &bull; {language === 'vi' ? 'Sức chứa:' : 'Capacity:'} <span className="font-semibold text-white">{sportsClass.enrolledCount || 0}/{sportsClass.capacity}</span>
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-white rounded-md hover:bg-slate-800 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="flex border-b border-slate-200 bg-slate-50 px-5 pt-3 gap-2">
          <button
            onClick={() => { setActiveTab('list'); setError(''); setSuccess(''); }}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-all ${
              activeTab === 'list' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-600 hover:text-slate-900'
            }`}
          >
            {language === 'vi' ? 'Danh sách buổi tập' : 'Session List'} ({classSessions.length})
          </button>
          <button
            onClick={() => { setActiveTab('create'); setError(''); setSuccess(''); }}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'create' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-600 hover:text-slate-900'
            }`}
          >
            <Plus className="w-3.5 h-3.5" />
            <span>{language === 'vi' ? 'Thêm 1 buổi' : 'Add Single Session'}</span>
          </button>
          <button
            onClick={() => { setActiveTab('generate'); setError(''); setSuccess(''); }}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'generate' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-600 hover:text-slate-900'
            }`}
          >
            <Repeat className="w-3.5 h-3.5" />
            <span>{language === 'vi' ? 'Xếp lịch định kỳ tự động' : 'Generate Recurring'}</span>
          </button>
        </div>

        {/* Messages */}
        {error && (
          <div className="mx-5 mt-4 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800 flex items-start gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold">{language === 'vi' ? 'Phát hiện lỗi / Xung đột:' : 'Error / Conflict Detected:'}</span>
              <p className="mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {success && (
          <div className="mx-5 mt-4 p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{success}</span>
          </div>
        )}

        {conflictWarning && conflictWarning.hasConflict && (
          <div className="mx-5 mt-4 p-3 bg-amber-50 border border-amber-300 rounded-lg text-xs text-amber-900 flex items-start gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold">{language === 'vi' ? 'Cảnh báo trùng lịch phòng hoặc HLV:' : 'Warning: Schedule Conflict Detected:'}</span>
              <ul className="list-disc pl-4 mt-1 space-y-0.5">
                {conflictWarning.conflictDetails.map((detail, idx) => (
                  <li key={idx}>{detail}</li>
                ))}
              </ul>
            </div>
          </div>
        )}

        {/* Body content */}
        <div className="p-5 overflow-y-auto flex-1">
          {activeTab === 'list' && (
            <div className="space-y-3">
              {classSessions.length === 0 ? (
                <div className="py-12 text-center text-slate-500 space-y-3">
                  <Calendar className="w-10 h-10 mx-auto text-slate-300" />
                  <p className="text-xs">
                    {language === 'vi' ? 'Lớp này chưa có buổi học cụ thể nào được xếp lịch.' : 'No sessions have been scheduled for this class yet.'}
                  </p>
                  <button
                    onClick={() => setActiveTab('generate')}
                    className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs font-bold inline-flex items-center gap-1.5 shadow-xs"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>{language === 'vi' ? 'Tạo lịch định kỳ ngay' : 'Generate Schedule Now'}</span>
                  </button>
                </div>
              ) : (
                classSessions.map(session => (
                  <div key={session.id} className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg flex items-center justify-between gap-3 hover:border-slate-300 transition-all">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-900">
                          {formatDate(session.sessionDate || session.date, language)}
                        </span>
                        <span className="px-2 py-0.5 text-[10px] font-mono font-bold bg-blue-50 text-blue-800 border border-blue-200 rounded">
                          {session.startTime} - {session.endTime}
                        </span>
                        <span className={`px-2 py-0.5 text-[10px] font-bold rounded uppercase ${
                          session.status === 'scheduled' ? 'bg-amber-100 text-amber-800' :
                          session.status === 'completed' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'
                        }`}>
                          {session.rawStatus || session.status}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-slate-600">
                        <span className="flex items-center gap-1">
                          <MapPin className="w-3.5 h-3.5 text-slate-400" />
                          <strong>{session.roomName || 'Phòng học'}</strong> ({session.roomLocation || ''})
                        </span>
                        <span>
                          {language === 'vi' ? 'HLV:' : 'Coach:'} {session.coachName || sportsClass.coachName}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {session.status === 'scheduled' && (
                        <button
                          onClick={() => updateSessionStatus(session.id, 'Completed')}
                          className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-[11px] font-bold shadow-2xs"
                        >
                          {language === 'vi' ? 'Hoàn tất' : 'Complete'}
                        </button>
                      )}
                      <button
                        onClick={() => deleteSession(session.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded transition-colors"
                        title={language === 'vi' ? 'Xóa buổi học' : 'Delete session'}
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {activeTab === 'create' && (
            <form onSubmit={handleCreateSingle} className="space-y-4">
              <div className="p-3 bg-blue-50/60 border border-blue-200 rounded-lg text-xs text-blue-900">
                <span className="font-bold">Thuật toán kiểm tra trùng lịch:</span> Hệ thống sẽ tự động đối chiếu thời gian bắt đầu và kết thúc với toàn bộ lịch phòng và lịch dạy của HLV để ngăn chặn trùng lặp.
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Ngày buổi học' : 'Session Date'}
                  </label>
                  <input
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={singleForm.sessionDate}
                    onChange={e => setSingleForm({ ...singleForm, sessionDate: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Phòng học' : 'Room'}
                  </label>
                  <select
                    value={singleForm.roomId}
                    onChange={e => setSingleForm({ ...singleForm, roomId: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  >
                    {rooms.map(r => (
                      <option key={r.id} value={r.id}>
                        {r.name} ({r.location}) - Sức chứa: {r.capacity} ({r.status.toUpperCase()})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Giờ bắt đầu' : 'Start Time'}
                  </label>
                  <input
                    type="time"
                    required
                    value={singleForm.startTime}
                    onChange={e => setSingleForm({ ...singleForm, startTime: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Giờ kết thúc' : 'End Time'}
                  </label>
                  <input
                    type="time"
                    required
                    value={singleForm.endTime}
                    onChange={e => setSingleForm({ ...singleForm, endTime: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-slate-200 flex items-center justify-between">
                <button
                  type="button"
                  onClick={handleCheckConflict}
                  className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded text-xs font-bold flex items-center gap-1.5 transition-all"
                >
                  <ShieldAlert className="w-3.5 h-3.5 text-blue-600" />
                  <span>{language === 'vi' ? 'Kiểm tra trùng phòng/HLV' : 'Check Conflict'}</span>
                </button>

                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs font-bold shadow-xs flex items-center gap-1.5 disabled:opacity-50"
                >
                  <Plus className="w-4 h-4" />
                  <span>{loading ? 'Đang lưu...' : (language === 'vi' ? 'Tạo buổi học' : 'Create Session')}</span>
                </button>
              </div>
            </form>
          )}

          {activeTab === 'generate' && (
            <form onSubmit={handleGenerateBatch} className="space-y-4">
              <div className="p-3 bg-blue-50/60 border border-blue-200 rounded-lg text-xs text-blue-900">
                <span className="font-bold">Tính năng Xếp lịch Tự động:</span> Hệ thống sẽ sinh lịch định kỳ trong khoảng thời gian lớp học theo các thứ trong tuần được chọn và kiểm tra trùng phòng/HLV trên từng buổi.
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Từ ngày' : 'Start Date'}
                  </label>
                  <input
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={batchForm.startDate}
                    onChange={e => setBatchForm({ ...batchForm, startDate: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Đến ngày' : 'End Date'}
                  </label>
                  <input
                    type="date"
                    required
                    min={batchForm.startDate}
                    value={batchForm.endDate}
                    onChange={e => setBatchForm({ ...batchForm, endDate: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-2">
                  {language === 'vi' ? 'Lặp lại vào các ngày trong tuần:' : 'Repeat on days of week:'}
                </label>
                <div className="flex flex-wrap gap-2">
                  {dayLabels.map(d => {
                    const isSelected = batchForm.daysOfWeek.includes(d.num);
                    return (
                      <button
                        key={d.num}
                        type="button"
                        onClick={() => toggleDayOfWeek(d.num)}
                        className={`px-3 py-1.5 rounded text-xs font-bold transition-all ${
                          isSelected
                            ? 'bg-blue-600 text-white shadow-xs'
                            : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                      >
                        {d.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Phòng học' : 'Room'}
                  </label>
                  <select
                    value={batchForm.roomId}
                    onChange={e => setBatchForm({ ...batchForm, roomId: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  >
                    {rooms.map(r => (
                      <option key={r.id} value={r.id}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Giờ bắt đầu' : 'Start Time'}
                  </label>
                  <input
                    type="time"
                    required
                    value={batchForm.startTime}
                    onChange={e => setBatchForm({ ...batchForm, startTime: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    {language === 'vi' ? 'Giờ kết thúc' : 'End Time'}
                  </label>
                  <input
                    type="time"
                    required
                    value={batchForm.endTime}
                    onChange={e => setBatchForm({ ...batchForm, endTime: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs font-semibold focus:outline-none focus:border-blue-600 focus:bg-white"
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-slate-200 flex justify-end">
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs font-bold shadow-xs flex items-center gap-1.5 disabled:opacity-50"
                >
                  <Sparkles className="w-4 h-4" />
                  <span>{loading ? 'Đang tạo lịch...' : (language === 'vi' ? 'Tự động tạo lịch học' : 'Generate Schedule')}</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
