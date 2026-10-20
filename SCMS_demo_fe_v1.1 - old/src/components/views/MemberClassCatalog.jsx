import React, { useState } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { formatDate } from '../../locales/translations';
import { StatusBadge } from '../common/Badge';
import { Users, Clock, MapPin, CheckCircle2, XCircle, AlertCircle } from 'lucide-react';

export function MemberClassCatalog() {
  const {
    classes,
    subjects,
    currentUser,
    bookClass,
    cancelClassBooking,
    enrollments,
    members,
    packages,
    t,
    language
  } = useSCMS();

  const [selectedCategory, setSelectedCategory] = useState('all');
  const [submittingId, setSubmittingId] = useState(null);

  const member = members.find(m => String(m.id) === String(currentUser?.id)) || members[0];
  const memberPkg = packages.find(p => String(p.id) === String(member?.currentPackageId));

  const filteredClasses = selectedCategory === 'all'
    ? classes
    : classes.filter(c => (c.category || '').toLowerCase() === selectedCategory.toLowerCase());

  const handleEnroll = async (cls) => {
    setSubmittingId(cls.id);
    try {
      await bookClass(cls.id, currentUser?.id, cls.nameVi || cls.name);
    } catch (err) {
      // toast shown in context
    } finally {
      setSubmittingId(null);
    }
  };

  const handleCancelEnroll = async (cls) => {
    setSubmittingId(cls.id);
    try {
      await cancelClassBooking(cls.id, currentUser?.id);
    } catch (err) {
      // toast shown in context
    } finally {
      setSubmittingId(null);
    }
  };

  const categories = ['all', ...subjects.map(s => s.name)];

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-white border border-slate-200 p-4 rounded-lg shadow-xs">
        <div>
          <h2 className="text-sm font-bold text-slate-900 tracking-tight">{t('classCatalog')}</h2>
          <p className="text-xs text-slate-500">{language === 'vi' ? 'Đăng ký tham gia lớp học nhóm trực tuyến theo thời gian thực' : 'Online real-time group class registration'}</p>
        </div>

        {/* Category Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto max-w-full pb-1">
          {categories.map(cat => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1 rounded text-xs font-bold whitespace-nowrap transition-all ${
                selectedCategory === cat ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:text-slate-900'
              }`}
            >
              {cat === 'all' ? (language === 'vi' ? 'Tất cả bộ môn' : 'All Categories') : cat}
            </button>
          ))}
        </div>
      </div>

      {/* Class Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredClasses.map(cls => {
          const isAlreadyBooked = enrollments.some(e =>
            String(e.classId) === String(cls.id) &&
            (String(e.memberId) === String(currentUser?.id) || String(e.memberId) === String(member?.id)) &&
            (e.status === 'registered' || e.status === 'Registered')
          );
          const isFull = (cls.enrolledCount || 0) >= (cls.capacity || cls.maxCapacity || 20);

          return (
            <div key={cls.id} className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-start justify-between">
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-bold uppercase border border-slate-200">
                    {cls.category || 'Fitness'}
                  </span>
                  <StatusBadge type="class" status={cls.status} />
                </div>

                <h3 className="text-sm font-bold text-slate-900 mt-2">{language === 'vi' ? (cls.nameVi || cls.name) : cls.name}</h3>

                <div className="mt-3 space-y-1.5 text-xs text-slate-700 bg-slate-50 p-3 rounded border border-slate-200">
                  <div className="flex items-center gap-2">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-mono tabular-nums">
                      {cls.startDate ? `${formatDate(cls.startDate, language)} - ${cls.endDate ? formatDate(cls.endDate, language) : ''}` : 'Chưa xếp ngày'}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <MapPin className="w-3.5 h-3.5 text-slate-400" />
                    <span>{language === 'vi' ? (cls.roomVi || cls.room || 'Phòng tập chung') : (cls.room || 'Main Studio')}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Users className="w-3.5 h-3.5 text-slate-400" />
                    <span>{language === 'vi' ? 'Huấn luyện viên' : 'Coach'}: <strong className="text-slate-900">{cls.coachName || 'Fitzone Coach'}</strong></span>
                  </div>
                </div>
              </div>

              {/* Booking Button & State Rules */}
              <div>
                <div className="flex items-center justify-between text-[11px] mb-2">
                  <span className="text-slate-500">{language === 'vi' ? 'Đã đăng ký:' : 'Enrolled:'}</span>
                  <span className={`font-mono font-bold tabular-nums ${isFull ? 'text-rose-700' : 'text-emerald-800'}`}>
                    {cls.enrolledCount || 0}/{cls.capacity || cls.maxCapacity || 20} {language === 'vi' ? 'học viên' : 'slots'}
                  </span>
                </div>

                {isAlreadyBooked ? (
                  <div className="space-y-1.5">
                    <div className="py-2 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded text-xs font-bold flex items-center justify-center gap-1.5">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>{language === 'vi' ? 'Đã đăng ký lớp này' : 'Enrolled in this class'}</span>
                    </div>
                    <button
                      disabled={submittingId === cls.id}
                      onClick={() => handleCancelEnroll(cls)}
                      className="w-full py-1 text-slate-500 hover:text-rose-600 text-[11px] font-semibold flex items-center justify-center gap-1 transition-all"
                    >
                      <XCircle className="w-3.5 h-3.5" />
                      <span>{language === 'vi' ? 'Hủy đăng ký lớp' : 'Cancel Enrollment'}</span>
                    </button>
                  </div>
                ) : isFull ? (
                  <button disabled className="w-full py-2 bg-rose-50 border border-rose-200 text-rose-800 rounded text-xs font-bold cursor-not-allowed">
                    {language === 'vi' ? 'Lớp đã đủ số lượng học viên' : 'Class is full'}
                  </button>
                ) : (
                  <button
                    disabled={submittingId === cls.id}
                    onClick={() => handleEnroll(cls)}
                    className="w-full py-2 bg-slate-900 hover:bg-slate-800 text-white rounded text-xs font-bold shadow-xs transition-all disabled:opacity-50 flex items-center justify-center gap-1.5"
                  >
                    <span>{submittingId === cls.id ? '...' : (language === 'vi' ? 'Đăng Ký Tham Gia' : 'Enroll Now')}</span>
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
