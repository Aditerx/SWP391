import React, { useState } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { formatDate } from '../../locales/translations';
import { Calendar, Users, Clock, CheckCircle2, AlertCircle, XCircle } from 'lucide-react';

export function ReceptionClassBooking() {
  const { classes, members, enrollments, bookClass, cancelClassBooking, t, language } = useSCMS();
  const [selectedMemberId, setSelectedMemberId] = useState(members[0]?.id || '');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [submittingId, setSubmittingId] = useState(null);

  const selectedMember = members.find(m => String(m.id) === String(selectedMemberId));

  const filteredClasses = categoryFilter === 'all'
    ? classes
    : classes.filter(c => (c.category || '').toLowerCase() === categoryFilter.toLowerCase());

  const handleDeskBooking = async (classId, className) => {
    if (!selectedMember) return;
    setSubmittingId(classId);
    try {
      await bookClass(classId, selectedMember.id, className);
    } catch (err) {
      // toast shown in context
    } finally {
      setSubmittingId(null);
    }
  };

  const handleDeskCancel = async (classId) => {
    if (!selectedMember) return;
    setSubmittingId(classId);
    try {
      await cancelClassBooking(classId, selectedMember.id);
    } catch (err) {
      // toast shown in context
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-800 rounded border border-blue-200">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900 whitespace-nowrap">{t('bookings')}</h2>
            <p className="text-xs text-slate-500 whitespace-nowrap">
              {language === 'vi' ? 'Đăng ký lịch lớp học trực tiếp tại quầy Lễ tân cho hội viên' : 'Register class schedules directly at the front desk for members'}
            </p>
          </div>
        </div>
      </div>

      {/* Select Member Bar */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs space-y-3">
        <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block">
          {language === 'vi' ? '1. Chọn Thành Viên Cần Đăng Ký Lớp:' : '1. Select Member:'}
        </label>
        <select
          value={selectedMemberId}
          onChange={e => setSelectedMemberId(e.target.value)}
          className="w-full bg-slate-50 border border-slate-300 rounded p-2.5 text-xs font-bold text-slate-900 focus:outline-none focus:border-blue-600 focus:bg-white"
        >
          {members.map(m => (
            <option key={m.id} value={m.id}>
              {m.name} ({m.code}) - {m.membershipStatus?.toUpperCase()} - {m.currentPackageName}
            </option>
          ))}
        </select>

        {selectedMember && (
          <div className="p-3 bg-slate-50 rounded border border-slate-200 text-xs flex items-center justify-between">
            <div>
              <span className="text-slate-500">{language === 'vi' ? 'Trạng thái tài khoản: ' : 'Account status: '}</span>
              <span className={`font-bold uppercase ${selectedMember.membershipStatus === 'active' ? 'text-emerald-700' : 'text-amber-700'}`}>
                {selectedMember.membershipStatus}
              </span>
            </div>
            {selectedMember.membershipStatus !== 'active' && (
              <span className="text-rose-700 font-semibold flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                {language === 'vi' ? 'Hội viên chưa có gói tập hợp lệ hoặc gói đã hết hạn' : 'Membership expired or not active'}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Classes Available */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">{language === 'vi' ? '2. Danh Sách Lớp Học' : '2. Available Classes'}</h3>
          <select
            value={categoryFilter}
            onChange={e => setCategoryFilter(e.target.value)}
            className="bg-slate-50 border border-slate-300 rounded px-3 py-1 text-xs text-slate-900 font-semibold"
          >
            <option value="all">{language === 'vi' ? 'Tất cả bộ môn' : 'All Categories'}</option>
            <option value="Yoga">Yoga</option>
            <option value="Fitness">Fitness</option>
            <option value="CrossFit">CrossFit</option>
            <option value="Pilates">Pilates</option>
            <option value="Gym cơ bản">Gym cơ bản</option>
            <option value="Boxing/Kickboxing">Boxing/Kickboxing</option>
            <option value="Cardio/Zumba">Cardio/Zumba</option>
          </select>
        </div>

        <div className="space-y-3">
          {filteredClasses.map(cls => {
            const isAlreadyEnrolled = enrollments.some(e =>
              String(e.classId) === String(cls.id) &&
              String(e.memberId) === String(selectedMember?.id) &&
              (e.status === 'registered' || e.status === 'Registered')
            );
            const isFull = (cls.enrolledCount || 0) >= (cls.capacity || cls.maxCapacity || 20);
            const canBook = selectedMember?.membershipStatus === 'active' && !isFull && !isAlreadyEnrolled;

            return (
              <div key={cls.id} className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 bg-blue-100 text-blue-900 border border-blue-200 text-[10px] font-mono font-bold rounded">
                      {cls.category || 'Fitness'}
                    </span>
                    <span className="text-sm font-bold text-slate-900">{language === 'vi' ? (cls.nameVi || cls.name) : cls.name}</span>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <span className="flex items-center gap-1 font-mono tabular-nums">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {cls.startDate ? `${formatDate(cls.startDate, language)} - ${cls.endDate ? formatDate(cls.endDate, language) : ''}` : 'Chưa xếp ngày'}
                    </span>
                    <span>{language === 'vi' ? 'HLV:' : 'Coach:'} <strong>{cls.coachName || 'Fitzone Coach'}</strong></span>
                    <span>{language === 'vi' ? 'Phòng:' : 'Room:'} <strong>{language === 'vi' ? (cls.roomVi || cls.room || 'Phòng tập chung') : (cls.room || 'Main Studio')}</strong></span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold text-slate-900 tabular-nums">
                    {cls.enrolledCount || 0}/{cls.capacity || cls.maxCapacity || 20} {language === 'vi' ? 'chỗ' : 'slots'}
                  </span>

                  {isAlreadyEnrolled ? (
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded border border-emerald-200">
                        {language === 'vi' ? 'Đã đăng ký' : 'Enrolled'}
                      </span>
                      <button
                        disabled={submittingId === cls.id}
                        onClick={() => handleDeskCancel(cls.id)}
                        className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded text-xs font-bold transition-all disabled:opacity-50"
                      >
                        {language === 'vi' ? 'Hủy lớp' : 'Cancel'}
                      </button>
                    </div>
                  ) : (
                    <button
                      disabled={!canBook || submittingId === cls.id}
                      onClick={() => handleDeskBooking(cls.id, language === 'vi' ? cls.nameVi : cls.name)}
                      className={`px-3.5 py-1.5 rounded text-xs font-bold shadow-xs flex items-center gap-1.5 ${
                        canBook
                          ? 'bg-slate-900 hover:bg-slate-800 text-white cursor-pointer'
                          : 'bg-slate-200 text-slate-500 cursor-not-allowed border border-slate-300'
                      }`}
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>{submittingId === cls.id ? '...' : (language === 'vi' ? 'Đăng Ký Thay Học Viên' : 'Enroll on Behalf')}</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
