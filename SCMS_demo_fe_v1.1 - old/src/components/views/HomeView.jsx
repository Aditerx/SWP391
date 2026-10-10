import React from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { Dumbbell, Award, Calendar, Users, ShieldCheck, Clock, MapPin, ArrowRight, Zap, Flame, HeartPulse } from 'lucide-react';

export function HomeView({ onSelectTab }) {
  const { role, currentUser, language, t } = useSCMS();

  const isVi = language === 'vi';

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs relative overflow-hidden">
        <div className="max-w-2xl space-y-3 relative z-10">
          <span className="px-3 py-1 bg-blue-50 text-blue-800 border border-blue-200 text-xs font-bold rounded-full uppercase tracking-wider inline-flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-blue-600" />
            <span>{isVi ? 'Trung Tâm Thể Thao Đẳng Cấp Quốc Tế' : 'Elite International Sports Complex'}</span>
          </span>

          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight leading-tight">
            {isVi
              ? `Chào mừng ${currentUser.name} đến với SCMS Elite Complex`
              : `Welcome ${currentUser.name} to SCMS Elite Complex`}
          </h1>

          <p className="text-sm text-slate-600 leading-relaxed">
            {isVi
              ? 'Hệ thống quản lý trung tâm thể thao toàn diện với trang thiết bị hiện đại, đội ngũ HLV chứng chỉ quốc tế và lộ trình tập luyện cá nhân hóa chuyên sâu.'
              : 'Comprehensive sports center management platform featuring state-of-the-art equipment, certified trainers, and personalized fitness programs.'}
          </p>

          <div className="pt-2 flex flex-wrap items-center gap-3">
            {role === 'member' && (
              <button
                onClick={() => onSelectTab && onSelectTab('member_class_catalog')}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-bold shadow-xs flex items-center gap-2 transition-all"
              >
                <span>{isVi ? 'Đăng Ký Lớp Học Ngay' : 'Book a Class Now'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}

            {role === 'coach' && (
              <button
                onClick={() => onSelectTab && onSelectTab('coach_schedule')}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-bold shadow-xs flex items-center gap-2 transition-all"
              >
                <span>{isVi ? 'Xem Lịch Dạy Hôm Nay' : 'View Teaching Schedule'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}

            {role === 'receptionist' && (
              <button
                onClick={() => onSelectTab && onSelectTab('reception_member_search')}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-bold shadow-xs flex items-center gap-2 transition-all"
              >
                <span>{isVi ? 'Tra Cứu Hồ Sơ Thành Viên' : 'Search Member Profile'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Key Center Highlights */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-blue-50 text-blue-800 rounded-lg border border-blue-200 flex-shrink-0">
            <Dumbbell className="w-5 h-5" />
          </div>
          <div>
            <div className="text-lg font-extrabold text-slate-900 font-mono tabular-nums">2,500 m²</div>
            <div className="text-xs font-medium text-slate-500">
              {isVi ? 'Diện tích phòng tập' : 'Total Gym Area'}
            </div>
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-emerald-50 text-emerald-800 rounded-lg border border-emerald-200 flex-shrink-0">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-lg font-extrabold text-slate-900 font-mono tabular-nums">15+ HLV</div>
            <div className="text-xs font-medium text-slate-500">
              {isVi ? 'Chứng chỉ quốc tế' : 'Certified Personal Trainers'}
            </div>
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-amber-50 text-amber-800 rounded-lg border border-amber-200 flex-shrink-0">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <div className="text-lg font-extrabold text-slate-900 font-mono tabular-nums">50+ Lớp/Tuần</div>
            <div className="text-xs font-medium text-slate-500">
              {isVi ? 'Yoga, CrossFit, Pilates' : 'Yoga, CrossFit, Pilates'}
            </div>
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-purple-50 text-purple-800 rounded-lg border border-purple-200 flex-shrink-0">
            <Award className="w-5 h-5" />
          </div>
          <div>
            <div className="text-lg font-extrabold text-slate-900 font-mono tabular-nums">99.8%</div>
            <div className="text-xs font-medium text-slate-500">
              {isVi ? 'Hài lòng thành viên' : 'Member Satisfaction'}
            </div>
          </div>
        </div>
      </div>

      {/* Studios & Facilities Overview */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
        <h2 className="text-base font-bold text-slate-900 border-b border-slate-200 pb-3">
          {isVi ? 'Khu Vực Tập Luyện & Cơ Sở Vật Chất' : 'Studios & Training Facilities'}
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
            <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
              <Flame className="w-4 h-4 text-rose-600" />
              <span>Studio A - Yoga &amp; Mind Body</span>
            </div>
            <p className="text-slate-600 leading-relaxed">
              {isVi
                ? 'Không gian yên tĩnh tích hợp thảm tập cao cấp, điều hòa lọc khí ion và hệ thống âm thanh vòm thư giãn.'
                : 'Peaceful ambiance with premium mats, ionized air purification, and immersive audio system.'}
            </p>
            <div className="text-[11px] font-mono text-slate-500 font-semibold">
              {isVi ? 'Sức chứa: 20 học viên' : 'Capacity: 20 members'}
            </div>
          </div>

          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
            <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
              <Dumbbell className="w-4 h-4 text-blue-600" />
              <span>Studio B - Fitness &amp; CrossFit</span>
            </div>
            <p className="text-slate-600 leading-relaxed">
              {isVi
                ? 'Trang bị dàn tạ đòn Technogym, sàn cao su giảm chấn 30mm và hệ thống tạ tay chuẩn Olympic.'
                : 'Equipped with Technogym barbells, 30mm shock-absorbent flooring, and Olympic dumbbells.'}
            </p>
            <div className="text-[11px] font-mono text-slate-500 font-semibold">
              {isVi ? 'Sức chứa: 25 học viên' : 'Capacity: 25 members'}
            </div>
          </div>

          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
            <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
              <HeartPulse className="w-4 h-4 text-emerald-600" />
              <span>Gym Main Zone &amp; Cardio</span>
            </div>
            <p className="text-slate-600 leading-relaxed">
              {isVi
                ? 'Máy chạy bộ, xe đạp tập, máy chèo thuyền tương tác màn hình cảm ứng và khu vực căng cơ chuyên dụng.'
                : 'Interactive treadmills, stationary bikes, rowing machines, and stretching zone.'}
            </p>
            <div className="text-[11px] font-mono text-slate-500 font-semibold">
              {isVi ? 'Giờ mở cửa: 06:00 - 22:00' : 'Hours: 06:00 - 22:00'}
            </div>
          </div>
        </div>
      </div>

      {/* Operating Information */}
      <div className="bg-slate-900 text-white rounded-xl p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="text-sm font-bold flex items-center gap-2">
            <Clock className="w-4 h-4 text-blue-400" />
            <span>{isVi ? 'Thời Gian Hoạt Động Trung Tâm' : 'Center Operating Hours'}</span>
          </div>
          <p className="text-xs text-slate-300">
            {isVi
              ? 'Thứ Hai - Chủ Nhật: 06:00 - 22:00 (Kể cả ngày lễ & Cuối tuần)'
              : 'Monday - Sunday: 06:00 - 22:00 (Open on holidays & weekends)'}
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs font-mono text-slate-300">
          <MapPin className="w-4 h-4 text-emerald-400" />
          <span>SCMS Elite Complex #01 &bull; TP. Hồ Chí Minh</span>
        </div>
      </div>
    </div>
  );
}
