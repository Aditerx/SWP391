import React, { useState } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { formatDate } from '../../locales/translations';
import { Calendar, Clock, MapPin, Users, Filter, CheckCircle2 } from 'lucide-react';

export function CoachScheduleView() {
  const { sessions, classes, rooms, currentUser, updateSessionStatus, t, language } = useSCMS();
  const [selectedRoom, setSelectedRoom] = useState('all');

  const mySessions = sessions.filter(s => {
    if (String(s.coachId) === String(currentUser?.id)) return true;
    const cls = classes.find(c => String(c.id) === String(s.classId));
    if (cls && String(cls.coachId) === String(currentUser?.id)) return true;
    return true;
  });

  const filteredSessions = selectedRoom === 'all'
    ? mySessions
    : mySessions.filter(s => String(s.roomId) === String(selectedRoom) || s.roomName === selectedRoom);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-800 rounded border border-blue-200">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900 whitespace-nowrap">{t('schedule')}</h2>
            <p className="text-xs text-slate-500 whitespace-nowrap">
              {language === 'vi' ? 'Lịch giảng dạy chi tiết, danh sách học viên từng buổi học & phòng tập' : 'Detailed teaching schedule, student roster and assigned rooms'}
            </p>
          </div>
        </div>

        {/* Room Filter */}
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-500" />
          <span className="text-xs font-bold text-slate-700">Lọc theo phòng:</span>
          <select
            value={selectedRoom}
            onChange={e => setSelectedRoom(e.target.value)}
            className="bg-slate-50 border border-slate-300 rounded px-3 py-1.5 text-xs text-slate-900 font-semibold focus:outline-none focus:border-blue-600"
          >
            <option value="all">{language === 'vi' ? 'Tất cả phòng' : 'All Rooms'}</option>
            {rooms.map(r => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Schedule Table / Roster List */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">Danh Sách Buổi Dạy Chi Tiết</h3>
          <span className="text-xs text-slate-500 font-mono">Tổng số: {filteredSessions.length} buổi</span>
        </div>

        <div className="space-y-3">
          {filteredSessions.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-500">
              {language === 'vi' ? 'Chưa có buổi dạy nào trong danh sách.' : 'No scheduled teaching sessions found.'}
            </div>
          ) : (
            filteredSessions.map(session => (
              <div key={session.id} className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-4 hover:border-slate-300 transition-all">
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 bg-blue-100 text-blue-900 border border-blue-200 text-[10px] font-mono font-bold rounded">
                      {session.subjectName || 'Lớp nhóm'}
                    </span>
                    <span className="text-sm font-bold text-slate-900">{session.className}</span>
                    <span className={`px-2 py-0.5 text-[10px] font-bold rounded uppercase ${
                      session.status === 'scheduled' || session.status === 'Scheduled' ? 'bg-amber-100 text-amber-800' :
                      session.status === 'completed' || session.status === 'Completed' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'
                    }`}>
                      {session.rawStatus || session.status}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <div className="flex items-center gap-1 font-mono tabular-nums">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{formatDate(session.sessionDate || session.date, language)} &bull; {session.startTime} - {session.endTime}</span>
                    </div>

                    <div className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span className="font-semibold">{session.roomName} ({session.roomLocation || ''})</span>
                    </div>

                    <div className="flex items-center gap-1">
                      <Users className="w-3.5 h-3.5 text-slate-400" />
                      <span className="font-mono tabular-nums font-bold text-slate-900">{session.enrolledCount || 0}/{session.maxCapacity || 20} {language === 'vi' ? 'Học viên' : 'Members'}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {(session.status === 'scheduled' || session.status === 'Scheduled') ? (
                    <button
                      onClick={() => updateSessionStatus(session.id, 'Completed')}
                      className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded text-xs font-bold shadow-xs flex items-center gap-1.5"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>{language === 'vi' ? 'Hoàn Tất & Điểm Danh' : 'Mark Completed'}</span>
                    </button>
                  ) : (
                    <span className="text-xs font-semibold text-emerald-700 flex items-center gap-1">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>{language === 'vi' ? 'Đã hoàn tất' : 'Completed'}</span>
                    </span>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
