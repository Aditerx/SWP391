import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { translations } from '../locales/translations.js';
import * as authApi from '../api/authApi.js';
import * as managerApi from '../api/managerApi.js';
import { getErrorMessage, UnauthorizedError } from '../api/apiErrors.js';
import { UNAUTHORIZED_EVENT } from '../api/httpClient.js';

const SCMSContext = createContext(undefined);
const API_ENABLED = import.meta.env.VITE_API_ENABLED !== 'false';

const roleFromApi = (role) => {
  const normalized = String(role || '').toLowerCase();
  if (normalized === 'admin') return 'admin';
  if (normalized === 'centermanager' || normalized === 'manager') return 'manager';
  if (normalized === 'coach') return 'coach';
  if (normalized === 'receptionist') return 'receptionist';
  if (normalized === 'member') return 'member';
  return 'manager';
};

// Initial Seed Data
const INITIAL_PACKAGES = [
  {
    id: 'pkg-1',
    name: 'Basic Fitness',
    nameVi: 'Gói Cơ Bản',
    price: 1200000,
    durationMonths: 1,
    durationDays: 30,
    description: 'Access to gym facilities and basic cardio area.',
    descriptionVi: 'Tập luyện không giới hạn tại khu vực tập tạ và máy chạy bộ.',
    benefits: [
      'Unlimited Gym & Cardio access',
      'Locker room & Sauna facilities',
      '1 free Personal Trainer orientation session'
    ],
    benefitsVi: [
      'Tập thể hình và máy chạy bộ không giới hạn',
      'Sử dụng tủ khóa cá nhân và phòng xông hơi',
      '1 buổi định hướng tập luyện cùng Huấn luyện viên'
    ],
    maxClassesPerMonth: 4,
    tier: 1,
    status: 'active'
  },
  {
    id: 'pkg-2',
    name: 'Pro Performance',
    nameVi: 'Gói Nâng Cao',
    price: 3200000,
    durationMonths: 3,
    durationDays: 90,
    description: 'Full access + group classes (Yoga, Pilates, CrossFit) + priority booking.',
    descriptionVi: 'Toàn quyền tập luyện, tham gia lớp nhóm và ưu tiên đặt chỗ trước.',
    benefits: [
      'Includes all Basic benefits',
      'Unlimited group fitness classes',
      'Priority booking during peak hours',
      'Periodic InBody body composition scan'
    ],
    benefitsVi: [
      'Bao gồm toàn bộ quyền lợi của Gói Cơ Bản',
      'Tham gia không giới hạn các lớp nhóm',
      'Ưu tiên đặt chỗ trước trong khung giờ cao điểm',
      'Đo và phân tích chỉ số cơ thể InBody định kỳ'
    ],
    maxClassesPerMonth: 16,
    tier: 2,
    status: 'active'
  },
  {
    id: 'pkg-3',
    name: 'VIP Elite Club',
    nameVi: 'Gói Thượng Hạng',
    price: 11500000,
    durationMonths: 12,
    durationDays: 365,
    description: 'Ultimate all-access membership + Personal Coach assigned + VIP lounge.',
    descriptionVi: 'Đặc quyền cao cấp nhất với Huấn luyện viên riêng và phòng chờ tiện nghi.',
    benefits: [
      'Includes all Pro benefits',
      'Dedicated Personal Trainer with custom plan',
      'Private VIP lounge & complimentary towels',
      'Unlimited access to all facilities & classes',
      '2 free guest passes per month'
    ],
    benefitsVi: [
      'Bao gồm toàn bộ quyền lợi của Gói Nâng Cao',
      'Huấn luyện viên cá nhân theo sát lộ trình riêng',
      'Phòng chờ riêng biệt và dịch vụ khăn tắm miễn phí',
      'Tham gia tất cả các lớp học không giới hạn',
      'Tặng 2 lượt vé mời người thân mỗi tháng'
    ],
    maxClassesPerMonth: 999,
    tier: 3,
    status: 'active'
  }
];

const INITIAL_STAFF = [
  { id: 'stf-1', code: 'MGR-001', name: 'Nguyễn Văn An', email: 'manager@scms.com', phone: '0901234501', role: 'manager', status: 'active' },
  { id: 'stf-2', code: 'COA-101', name: 'Trần Thị Hương', email: 'huong.tran@fitzone.vn', phone: '0901234502', role: 'coach', specialty: 'Yoga & Pilates', specialtyVi: 'Yoga & Pilates', status: 'active', activeClassesCount: 8 },
  { id: 'stf-3', code: 'COA-102', name: 'Lê Minh Khoa', email: 'khoa.le@fitzone.vn', phone: '0901234503', role: 'coach', specialty: 'Gym, Tăng cơ giảm mỡ', specialtyVi: 'Gym & Tăng cơ', status: 'active', activeClassesCount: 12 },
  { id: 'stf-4', code: 'COA-103', name: 'Phạm Thị Lan', email: 'lan.pham@fitzone.vn', phone: '0901234504', role: 'coach', specialty: 'Boxing & Kickboxing', specialtyVi: 'Boxing & Võ thuật', status: 'active', activeClassesCount: 6 },
  { id: 'stf-5', code: 'REC-201', name: 'Đỗ Thị Mai', email: 'mai.do@fitzone.vn', phone: '0901234505', role: 'receptionist', status: 'active' },
  { id: 'stf-6', code: 'REC-202', name: 'Vũ Văn Nam', email: 'nam.vu@fitzone.vn', phone: '0901234506', role: 'receptionist', status: 'active' }
];

const INITIAL_MEMBERS = [
  { id: 'mem-1', code: 'MB-1007', name: 'Hoàng Thị Oanh', email: 'oanh.hoang@fitzone.vn', phone: '0901234507', joinDate: '2026-08-01', currentPackageId: 'pkg-2', currentPackageName: 'Gói Tiêu Chuẩn 3 Tháng', membershipStatus: 'active', totalSpent: 1350000 },
  { id: 'mem-2', code: 'MB-1008', name: 'Bùi Văn Phúc', email: 'phuc.bui@fitzone.vn', phone: '0901234508', joinDate: '2026-09-01', currentPackageId: 'pkg-1', currentPackageName: 'Gói Cơ Bản 1 Tháng', membershipStatus: 'active', totalSpent: 500000 },
  { id: 'mem-3', code: 'MB-1009', name: 'Ngô Thị Quyên', email: 'quyen.ngo@fitzone.vn', phone: '0901234509', joinDate: '2026-06-15', currentPackageId: 'pkg-3', currentPackageName: 'Gói Cao Cấp 6 Tháng', membershipStatus: 'active', totalSpent: 2500000 },
  { id: 'mem-4', code: 'MB-1010', name: 'Đặng Văn Sơn', email: 'son.dang@fitzone.vn', phone: '0901234510', joinDate: '2026-09-05', currentPackageId: 'pkg-2', currentPackageName: 'Gói Tiêu Chuẩn 3 Tháng', membershipStatus: 'active', totalSpent: 1350000 },
  { id: 'mem-5', code: 'MB-1011', name: 'Lý Thị Thu', email: 'thu.ly@fitzone.vn', phone: '0901234511', joinDate: '2026-08-15', currentPackageId: 'pkg-1', currentPackageName: 'Gói Cơ Bản 1 Tháng', membershipStatus: 'expired', totalSpent: 500000 },
  { id: 'mem-6', code: 'MB-1012', name: 'Trịnh Văn Vinh', email: 'vinh.trinh@fitzone.vn', phone: '0901234512', joinDate: '2026-01-10', currentPackageId: 'pkg-3', currentPackageName: 'Gói VIP 12 Tháng', membershipStatus: 'active', totalSpent: 4800000 }
];

const INITIAL_USERS = [
  { id: 1, code: 'ADM-1001', fullName: 'Hệ Thống Admin', email: 'admin@scms.com', phone: '0900000001', roleId: 1, roleName: 'Admin', role: 'admin', status: 'active', rawStatus: 'Active' },
  { id: 2, code: 'MGR-1002', fullName: 'Nguyễn Văn An', email: 'manager@scms.com', phone: '0901234501', roleId: 2, roleName: 'CenterManager', role: 'manager', status: 'active', rawStatus: 'Active' },
  { id: 3, code: 'COA-1003', fullName: 'Trần Thị Hương', email: 'huong.tran@fitzone.vn', phone: '0901234502', roleId: 3, roleName: 'Coach', role: 'coach', status: 'active', rawStatus: 'Active' },
  { id: 4, code: 'COA-1004', fullName: 'Lê Minh Khoa', email: 'khoa.le@fitzone.vn', phone: '0901234503', roleId: 3, roleName: 'Coach', role: 'coach', status: 'active', rawStatus: 'Active' },
  { id: 5, code: 'COA-1005', fullName: 'Phạm Thị Lan', email: 'lan.pham@fitzone.vn', phone: '0901234504', roleId: 3, roleName: 'Coach', role: 'coach', status: 'active', rawStatus: 'Active' },
  { id: 6, code: 'REC-1006', fullName: 'Đỗ Thị Mai', email: 'mai.do@fitzone.vn', phone: '0901234505', roleId: 4, roleName: 'Receptionist', role: 'receptionist', status: 'active', rawStatus: 'Active' },
  { id: 7, code: 'REC-1007', fullName: 'Vũ Văn Nam', email: 'nam.vu@fitzone.vn', phone: '0901234506', roleId: 4, roleName: 'Receptionist', role: 'receptionist', status: 'active', rawStatus: 'Active' },
  { id: 8, code: 'MB-1008', fullName: 'Hoàng Thị Oanh', email: 'oanh.hoang@fitzone.vn', phone: '0901234507', roleId: 5, roleName: 'Member', role: 'member', status: 'active', rawStatus: 'Active' },
  { id: 9, code: 'MB-1009', fullName: 'Bùi Văn Phúc', email: 'phuc.bui@fitzone.vn', phone: '0901234508', roleId: 5, roleName: 'Member', role: 'member', status: 'active', rawStatus: 'Active' },
  { id: 10, code: 'MB-1010', fullName: 'Ngô Thị Quyên', email: 'quyen.ngo@fitzone.vn', phone: '0901234509', roleId: 5, roleName: 'Member', role: 'member', status: 'active', rawStatus: 'Active' }
];

const INITIAL_ROLES = [
  { id: 1, name: 'Admin', description: 'Quản trị viên hệ thống - quản trị user và phân quyền RBAC', permissions: [{ id: 1, name: 'MANAGE_USERS', description: 'Quản lý người dùng' }, { id: 2, name: 'MANAGE_RBAC', description: 'Phân quyền vai trò hệ thống' }, { id: 5, name: 'VIEW_REPORTS', description: 'Xem báo cáo' }, { id: 10, name: 'VIEW_AUDIT_LOG', description: 'Xem nhật ký hệ thống' }] },
  { id: 2, name: 'CenterManager', description: 'Quản lý trung tâm - vận hành nghiệp vụ, lớp học, gói tập, báo cáo', permissions: [{ id: 1, name: 'MANAGE_USERS', description: 'Quản lý người dùng' }, { id: 3, name: 'MANAGE_CLASSES', description: 'Quản lý lớp học' }, { id: 4, name: 'MANAGE_PACKAGES', description: 'Quản lý gói tập' }, { id: 5, name: 'VIEW_REPORTS', description: 'Xem báo cáo' }, { id: 8, name: 'PROCESS_PAYMENT', description: 'Xử lý thanh toán' }, { id: 9, name: 'HANDLE_SUPPORT', description: 'Xử lý hỗ trợ' }, { id: 10, name: 'VIEW_AUDIT_LOG', description: 'Xem nhật ký hệ thống' }] },
  { id: 3, name: 'Coach', description: 'Huấn luyện viên - quản lý kế hoạch tập và điểm danh', permissions: [{ id: 6, name: 'MANAGE_TRAINING_PLAN', description: 'Quản lý kế hoạch tập' }, { id: 7, name: 'RECORD_RESULT', description: 'Ghi nhận kết quả & điểm danh' }] },
  { id: 4, name: 'Receptionist', description: 'Lễ tân - tiếp đón, đăng ký hội viên tại quầy, gói tập và thanh toán', permissions: [{ id: 7, name: 'RECORD_RESULT', description: 'Điểm danh tại quầy' }, { id: 8, name: 'PROCESS_PAYMENT', description: 'Xử lý thanh toán' }, { id: 9, name: 'HANDLE_SUPPORT', description: 'Hỗ trợ hội viên' }, { id: 11, name: 'REGISTER_MEMBER', description: 'Đăng ký học viên tại quầy' }, { id: 12, name: 'MANAGE_SUBSCRIPTIONS', description: 'Đăng ký & Gia hạn gói tập' }] },
  { id: 5, name: 'Member', description: 'Thành viên / Học viên trung tâm', permissions: [] }
];

const INITIAL_PERMISSIONS = [
  { id: 1, name: 'MANAGE_USERS', description: 'Quản lý người dùng hệ thống' },
  { id: 2, name: 'MANAGE_RBAC', description: 'Phân quyền vai trò hệ thống (RBAC)' },
  { id: 3, name: 'MANAGE_CLASSES', description: 'Quản lý lớp học' },
  { id: 4, name: 'MANAGE_PACKAGES', description: 'Quản lý gói tập' },
  { id: 5, name: 'VIEW_REPORTS', description: 'Xem báo cáo & thống kê doanh thu' },
  { id: 6, name: 'MANAGE_TRAINING_PLAN', description: 'Tạo và cập nhật kế hoạch tập luyện' },
  { id: 7, name: 'RECORD_RESULT', description: 'Ghi nhận kết quả tập luyện & điểm danh' },
  { id: 8, name: 'PROCESS_PAYMENT', description: 'Ghi nhận thanh toán, xuất hóa đơn' },
  { id: 9, name: 'HANDLE_SUPPORT', description: 'Xử lý yêu cầu hỗ trợ của hội viên' },
  { id: 10, name: 'VIEW_AUDIT_LOG', description: 'Xem nhật ký thao tác hệ thống' },
  { id: 11, name: 'REGISTER_MEMBER', description: 'Đăng ký học viên tại quầy' },
  { id: 12, name: 'MANAGE_SUBSCRIPTIONS', description: 'Đăng ký và gia hạn gói tập' }
];

const INITIAL_CLASSES = [
  { id: 1, code: 'CLS-YG-01', name: 'Yoga Buổi Sáng', nameVi: 'Yoga Buổi Sáng', category: 'Yoga', coachId: 3, coachName: 'Trần Thị Hương', room: 'Phòng Yoga A', roomVi: 'Phòng Yoga A', date: '2026-09-24', startTime: '06:00', durationMinutes: 60, capacity: 20, enrolledCount: 2, status: 'ongoing', minTierRequired: 1 },
  { id: 2, code: 'CLS-CF-02', name: 'Gym Sức Mạnh', nameVi: 'Gym Sức Mạnh', category: 'Gym cơ bản', coachId: 4, coachName: 'Lê Minh Khoa', room: 'Phòng Gym B', roomVi: 'Phòng Gym B', date: '2026-09-25', startTime: '18:00', durationMinutes: 90, capacity: 25, enrolledCount: 2, status: 'ongoing', minTierRequired: 1 },
  { id: 3, code: 'CLS-BX-03', name: 'Boxing Cơ Bản', nameVi: 'Boxing Cơ Bản', category: 'Boxing/Kickboxing', coachId: 5, coachName: 'Phạm Thị Lan', room: 'Phòng Võ C', roomVi: 'Phòng Võ C', date: '2026-09-26', startTime: '19:00', durationMinutes: 60, capacity: 15, enrolledCount: 2, status: 'open', minTierRequired: 1 },
  { id: 4, code: 'CLS-PL-04', name: 'Zumba Cardio', nameVi: 'Zumba Cardio', category: 'Cardio/Zumba', coachId: 3, coachName: 'Trần Thị Hương', room: 'Phòng Cardio D', roomVi: 'Phòng Cardio D', date: '2026-09-27', startTime: '17:00', durationMinutes: 60, capacity: 25, enrolledCount: 2, status: 'ongoing', minTierRequired: 1 }
];

const INITIAL_SUBJECTS = [
  { id: 1, name: 'Yoga', description: 'Các lớp Yoga cho mọi trình độ, tăng sự dẻo dai và thư giãn tinh thần' },
  { id: 2, name: 'Gym cơ bản', description: 'Tập luyện sức mạnh, tăng cơ giảm mỡ với thiết bị phòng gym' },
  { id: 3, name: 'Boxing/Kickboxing', description: 'Rèn luyện thể lực và kỹ thuật võ thuật đối kháng' },
  { id: 4, name: 'Cardio/Zumba', description: 'Các lớp vận động cường độ cao giúp đốt calo, cải thiện tim mạch' },
];

const INITIAL_ROOMS = [
  { id: 1, name: 'Phòng Yoga A', location: 'Tầng 2', capacity: 20, status: 'available' },
  { id: 2, name: 'Phòng Gym B', location: 'Tầng 1', capacity: 30, status: 'available' },
  { id: 3, name: 'Phòng Võ C', location: 'Tầng 3', capacity: 15, status: 'available' },
  { id: 4, name: 'Phòng Cardio D', location: 'Tầng 2', capacity: 25, status: 'available' },
];

const INITIAL_SESSIONS = [
  { id: 1, classId: 1, className: 'Yoga Buổi Sáng', subjectId: 1, subjectName: 'Yoga', coachId: 3, coachName: 'Trần Thị Hương', roomId: 1, roomName: 'Phòng Yoga A', roomLocation: 'Tầng 2', sessionDate: '2026-09-24', date: '2026-09-24', startTime: '06:00', endTime: '07:00', status: 'scheduled', rawStatus: 'Scheduled', enrolledCount: 2, maxCapacity: 20 },
  { id: 2, classId: 2, className: 'Gym Sức Mạnh', subjectId: 2, subjectName: 'Gym cơ bản', coachId: 4, coachName: 'Lê Minh Khoa', roomId: 2, roomName: 'Phòng Gym B', roomLocation: 'Tầng 1', sessionDate: '2026-09-25', date: '2026-09-25', startTime: '18:00', endTime: '19:30', status: 'scheduled', rawStatus: 'Scheduled', enrolledCount: 2, maxCapacity: 25 },
  { id: 3, classId: 3, className: 'Boxing Cơ Bản', subjectId: 3, subjectName: 'Boxing/Kickboxing', coachId: 5, coachName: 'Phạm Thị Lan', roomId: 3, roomName: 'Phòng Võ C', roomLocation: 'Tầng 3', sessionDate: '2026-09-26', date: '2026-09-26', startTime: '19:00', endTime: '20:00', status: 'scheduled', rawStatus: 'Scheduled', enrolledCount: 2, maxCapacity: 15 },
  { id: 4, classId: 4, className: 'Zumba Cardio', subjectId: 4, subjectName: 'Cardio/Zumba', coachId: 3, coachName: 'Trần Thị Hương', roomId: 4, roomName: 'Phòng Cardio D', roomLocation: 'Tầng 2', sessionDate: '2026-09-27', date: '2026-09-27', startTime: '17:00', endTime: '18:00', status: 'scheduled', rawStatus: 'Scheduled', enrolledCount: 2, maxCapacity: 25 },
];

const INITIAL_ENROLLMENTS = [
  { id: 1, memberId: 8, memberName: 'Hoàng Thị Oanh', memberCode: 'MB-1008', classId: 1, className: 'Yoga Buổi Sáng', status: 'registered', rawStatus: 'Registered' },
  { id: 2, memberId: 10, memberName: 'Ngô Thị Quyên', memberCode: 'MB-1010', classId: 1, className: 'Yoga Buổi Sáng', status: 'registered', rawStatus: 'Registered' },
  { id: 3, memberId: 9, memberName: 'Bùi Văn Phúc', memberCode: 'MB-1009', classId: 2, className: 'Gym Sức Mạnh', status: 'registered', rawStatus: 'Registered' },
  { id: 4, memberId: 8, memberName: 'Hoàng Thị Oanh', memberCode: 'MB-1008', classId: 4, className: 'Zumba Cardio', status: 'registered', rawStatus: 'Registered' },
];

const INITIAL_BOOKINGS = [
  { id: 'bk-1', classId: 1, className: 'Yoga Buổi Sáng', memberId: 8, memberName: 'Hoàng Thị Oanh', bookingDate: '2026-09-18 10:30', status: 'confirmed' },
  { id: 'bk-2', classId: 2, className: 'Gym Sức Mạnh', memberId: 9, memberName: 'Bùi Văn Phúc', bookingDate: '2026-09-18 14:15', status: 'confirmed' }
];

const INITIAL_ATTENDANCE = [
  { id: 'att-1', classId: 1, memberId: 8, memberName: 'Hoàng Thị Oanh', status: 'present', markedAt: '2026-09-20 06:10', markedBy: 'Trần Thị Hương' },
  { id: 'att-2', classId: 2, memberId: 9, memberName: 'Bùi Văn Phúc', status: 'present', markedAt: '2026-09-20 18:05', markedBy: 'Lê Minh Khoa' }
];

const INITIAL_PAYMENTS = [
  { id: 'pay-101', transactionNo: 'TXN-70001', code: 'TXN-70001', memberId: 8, memberName: 'Hoàng Thị Oanh', packageId: 'pkg-2', packageName: 'Gói Tiêu Chuẩn 3 Tháng', amount: 1350000, method: 'bank_transfer', paymentDate: '2026-08-01 09:15', status: 'successful', processedBy: 'Đỗ Thị Mai', createdAt: '2026-08-01 09:15' },
  { id: 'pay-102', transactionNo: 'TXN-70002', code: 'TXN-70002', memberId: 9, memberName: 'Bùi Văn Phúc', packageId: 'pkg-1', packageName: 'Gói Cơ Bản 1 Tháng', amount: 500000, method: 'cash', paymentDate: '2026-09-01 14:20', status: 'successful', processedBy: 'Vũ Văn Nam', createdAt: '2026-09-01 14:20' }
];

const INITIAL_TRAINING_PLANS = [
  {
    id: 1,
    planId: 1,
    coachId: 3,
    coachName: 'Trần Thị Hương',
    classId: 1,
    className: 'Yoga Buổi Sáng',
    memberId: null,
    memberName: null,
    title: 'Giáo trình Yoga 4 tuần cho người mới',
    content: 'Tuần 1-2: các tư thế cơ bản; Tuần 3-4: nâng cao độ khó',
    goal: 'Tăng độ dẻo dai, giảm căng thẳng',
    goalVi: 'Tăng độ dẻo dai, giảm căng thẳng',
    startDate: '2026-09-01',
    endDate: '2026-09-28',
    progressPercent: 75,
    exercises: [
      { name: 'Tư thế Mèo-Bò (Cat-Cow)', sets: 3, reps: '10 nhịp', weight: 'Thảm tập' },
      { name: 'Tư thế Chiến binh (Warrior Pose)', sets: 3, reps: '45 giây', weight: 'Bodyweight' },
      { name: 'Tư thế Cây cầu (Bridge Pose)', sets: 3, reps: '1 phút', weight: 'Thảm tập' }
    ],
    resultNotes: 'Học viên tiến bộ tốt, giữ vững form chuẩn.'
  },
  {
    id: 2,
    planId: 2,
    coachId: 4,
    coachName: 'Lê Minh Khoa',
    classId: 2,
    className: 'Gym Sức Mạnh',
    memberId: null,
    memberName: null,
    title: 'Giáo trình tăng cơ 8 tuần',
    content: 'Chia lịch tập theo nhóm cơ, kết hợp dinh dưỡng',
    goal: 'Tăng khối lượng cơ nạc',
    goalVi: 'Tăng khối lượng cơ nạc',
    startDate: '2026-09-01',
    endDate: '2026-10-27',
    progressPercent: 60,
    exercises: [
      { name: 'Bench Press', sets: 4, reps: '8-10 reps', weight: '40-60kg' },
      { name: 'Barbell Squat', sets: 4, reps: '8-10 reps', weight: '50-70kg' },
      { name: 'Deadlift', sets: 3, reps: '6-8 reps', weight: '60-80kg' }
    ],
    resultNotes: 'Tăng đều mức tạ qua từng tuần.'
  },
  {
    id: 3,
    planId: 3,
    coachId: 4,
    coachName: 'Lê Minh Khoa',
    classId: null,
    className: null,
    memberId: 10,
    memberName: 'Đặng Văn Sơn',
    title: 'Kế hoạch giảm cân cá nhân - Sơn',
    content: 'Kết hợp cardio và tập tạ nhẹ, kiểm soát calo',
    goal: 'Giảm 5kg trong 2 tháng',
    goalVi: 'Giảm 5kg trong 2 tháng',
    startDate: '2026-09-05',
    endDate: '2026-11-05',
    progressPercent: 70,
    exercises: [
      { name: 'Treadmill Incline Walk', sets: 1, reps: '20 phút', weight: 'Độ dốc 6' },
      { name: 'Dumbbell Lunges', sets: 3, reps: '12 reps/chân', weight: '8kg/tay' },
      { name: 'Plank', sets: 3, reps: '60 giây', weight: 'Bodyweight' }
    ],
    resultNotes: 'Đã giảm 1.5kg sau 2 tuần đầu, tuân thủ ăn uống tốt.'
  }
];

const INITIAL_EVALUATIONS = [
  { id: 1, evaluationId: 1, memberId: 8, memberName: 'Hoàng Thị Oanh', memberCode: 'MB-1008', coachId: 3, coachName: 'Trần Thị Hương', evaluationDate: '2026-09-20', comment: 'Tiến bộ rõ rệt về độ dẻo dai sau 3 tuần, cần chú ý thêm nhịp thở khi giữ tư thế.', progressScore: 7.5 },
  { id: 2, evaluationId: 2, memberId: 9, memberName: 'Bùi Văn Phúc', memberCode: 'MB-1009', coachId: 4, coachName: 'Lê Minh Khoa', evaluationDate: '2026-09-22', comment: 'Tăng đều mức tạ qua từng buổi, thể lực cải thiện tốt.', progressScore: 8.0 },
  { id: 3, evaluationId: 3, memberId: 10, memberName: 'Đặng Văn Sơn', memberCode: 'MB-1010', coachId: 4, coachName: 'Lê Minh Khoa', evaluationDate: '2026-09-23', comment: 'Cần cải thiện việc đi trễ, kỹ thuật nâng tạ đã ổn định hơn.', progressScore: 6.5 }
];

const INITIAL_AUDIT_LOGS = [
  { id: 'log-1', timestamp: '2026-09-28 09:00:00', actorId: 1, actorName: 'Hệ Thống Admin (Quản trị viên)', actorRole: 'admin', action: 'UPDATE_ROLE_PERMISSIONS', target: 'Vai trò: Receptionist', previousValue: 'Old Permissions', newValue: 'REGISTER_MEMBER, MANAGE_SUBSCRIPTIONS...', reason: 'Cấp quyền đăng ký và gia hạn gói tập tại quầy cho lễ tân' },
  { id: 'log-2', timestamp: '2026-09-28 09:15:22', actorId: 6, actorName: 'Đỗ Thị Mai (Lễ tân)', actorRole: 'receptionist', action: 'SUBSCRIBE_PACKAGE', target: 'Hội viên Hoàng Thị Oanh', previousValue: 'None', newValue: 'Active', reason: 'Đăng ký Gói Tiêu Chuẩn 3 Tháng tại quầy' },
];

export const SCMSProvider = ({ children }) => {
  const [language, setLanguage] = useState('vi');
  const [role, setRole] = useState('manager');
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [toast, setToast] = useState(null);
  const [authUser, setAuthUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(API_ENABLED);
  const [localAuthenticated, setLocalAuthenticated] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [dataError, setDataError] = useState(null);

  // Dynamic default tab based on role
  useEffect(() => {
    if (role === 'admin') setCurrentTab('admin_users');
    else if (role === 'coach') setCurrentTab('coach_dashboard');
    else if (role === 'receptionist') setCurrentTab('reception_dashboard');
    else if (role === 'member') setCurrentTab('member_dashboard');
    else setCurrentTab('dashboard');
  }, [role]);

  // States
  const [users, setUsers] = useState(INITIAL_USERS);
  const [roles, setRoles] = useState(INITIAL_ROLES);
  const [permissions, setPermissions] = useState(INITIAL_PERMISSIONS);
  const [members, setMembers] = useState(INITIAL_MEMBERS);
  const [staff, setStaff] = useState(INITIAL_STAFF);
  const [packages, setPackages] = useState(API_ENABLED ? [] : INITIAL_PACKAGES);
  const [classes, setClasses] = useState(API_ENABLED ? [] : INITIAL_CLASSES);
  const [subjects, setSubjects] = useState(API_ENABLED ? [] : INITIAL_SUBJECTS);
  const [rooms, setRooms] = useState(API_ENABLED ? [] : INITIAL_ROOMS);
  const [sessions, setSessions] = useState(API_ENABLED ? [] : INITIAL_SESSIONS);
  const [enrollments, setEnrollments] = useState(API_ENABLED ? [] : INITIAL_ENROLLMENTS);
  const [dashboard, setDashboard] = useState(null);
  const [bookings, setBookings] = useState(INITIAL_BOOKINGS);
  const [attendance, setAttendance] = useState(INITIAL_ATTENDANCE);
  const [payments, setPayments] = useState(INITIAL_PAYMENTS);
  const [trainingPlans, setTrainingPlans] = useState(INITIAL_TRAINING_PLANS);
  const [evaluations, setEvaluations] = useState(INITIAL_EVALUATIONS);
  const [trainingResults, setTrainingResults] = useState([]);
  const [revenueReport, setRevenueReport] = useState(null);
  const [memberReport, setMemberReport] = useState(null);
  const [auditLogs, setAuditLogs] = useState(API_ENABLED ? [] : INITIAL_AUDIT_LOGS);

  const fallbackUser = {
    id: role === 'admin' ? 1 : role === 'coach' ? 3 : role === 'receptionist' ? 6 : role === 'member' ? 8 : 2,
    name: role === 'admin' ? 'Hệ Thống Admin' : role === 'coach' ? 'Trần Thị Hương' : role === 'receptionist' ? 'Đỗ Thị Mai' : role === 'member' ? 'Hoàng Thị Oanh' : 'Nguyễn Văn An',
    email: role === 'admin' ? 'admin@scms.com' : `${role}@scms.com`,
    role
  };

  const currentUser = authUser ? {
    id: authUser.userId,
    name: authUser.fullName,
    email: authUser.email,
    role: roleFromApi(authUser.role),
    authorities: authUser.authorities || []
  } : fallbackUser;

  const isAuthenticated = API_ENABLED ? Boolean(authUser) : localAuthenticated;

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  // Auth restore
  useEffect(() => {
    if (!API_ENABLED) return undefined;

    let active = true;
    authApi.getCurrentUser()
      .then((user) => {
        if (!active) return;
        setAuthUser(user);
        setRole(roleFromApi(user.role));
      })
      .catch((error) => {
        if (active && !(error instanceof UnauthorizedError)) {
          setDataError(getErrorMessage(error));
        }
      })
      .finally(() => active && setAuthLoading(false));

    const handleUnauthorized = () => setAuthUser(null);
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => {
      active = false;
      window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
    };
  }, []);

  // Data fetching based on authenticated role
  useEffect(() => {
    if (!API_ENABLED || !authUser) return undefined;

    let active = true;
    const currentRole = roleFromApi(authUser.role);
    setDataLoading(true);
    setDataError(null);

    (async () => {
      try {
        if (currentRole === 'admin') {
          const [loadedUsers, loadedRoles, loadedPerms, loadedAudit, loadedDash, loadedSessions, loadedEnrollments, loadedClasses, loadedInvoices, loadedRevReport, loadedMemReport] = await Promise.all([
            managerApi.getUsers(),
            managerApi.getRoles(),
            managerApi.getPermissions(),
            managerApi.getAuditLogs(),
            managerApi.getDashboard().catch(() => null),
            managerApi.getSessions().catch(() => []),
            managerApi.getEnrollments().catch(() => []),
            managerApi.getClasses({ subjects: [], coaches: [] }).catch(() => []),
            managerApi.getInvoices().catch(() => []),
            managerApi.getRevenueReport().catch(() => null),
            managerApi.getMemberReport().catch(() => null),
          ]);
          if (!active) return;
          setUsers(loadedUsers);
          setRoles(loadedRoles);
          setPermissions(loadedPerms);
          setAuditLogs(loadedAudit);
          setSessions(loadedSessions);
          setEnrollments(loadedEnrollments);
          setClasses(loadedClasses);
          if (loadedInvoices.length > 0) setPayments(loadedInvoices);
          if (loadedRevReport) setRevenueReport(loadedRevReport);
          if (loadedMemReport) setMemberReport(loadedMemReport);
          if (loadedDash) setDashboard(loadedDash);
        } else if (currentRole === 'manager') {
          const loadedSubjects = await managerApi.getSubjects();
          const [loadedRooms, loadedStaff, loadedMembers, loadedClasses, loadedPackages, loadedDashboard, loadedAuditLogs, loadedSessions, loadedEnrollments, loadedInvoices, loadedPlans, loadedAttendances, loadedEvaluations, loadedRevReport, loadedMemReport] = await Promise.all([
            managerApi.getRooms(),
            managerApi.getStaff(),
            managerApi.getMembers(),
            managerApi.getClasses({ subjects: loadedSubjects, coaches: [] }),
            managerApi.getPackages(),
            managerApi.getDashboard(),
            managerApi.getAuditLogs(),
            managerApi.getSessions().catch(() => []),
            managerApi.getEnrollments().catch(() => []),
            managerApi.getInvoices().catch(() => []),
            managerApi.getTrainingPlans().catch(() => []),
            managerApi.getAttendances().catch(() => []),
            managerApi.getEvaluations().catch(() => []),
            managerApi.getRevenueReport().catch(() => null),
            managerApi.getMemberReport().catch(() => null),
          ]);
          if (!active) return;
          setSubjects(loadedSubjects);
          setRooms(loadedRooms);
          setStaff(loadedStaff);
          setMembers(loadedMembers);
          setClasses(loadedClasses.map(cls => {
            const coach = loadedStaff.find(s => String(s.id) === String(cls.coachId));
            return coach ? { ...cls, coachName: coach.name } : cls;
          }));
          setPackages(loadedPackages);
          setSessions(loadedSessions);
          setEnrollments(loadedEnrollments);
          if (loadedInvoices.length > 0) setPayments(loadedInvoices);
          if (loadedPlans.length > 0) setTrainingPlans(loadedPlans);
          if (loadedAttendances.length > 0) setAttendance(loadedAttendances);
          if (loadedEvaluations.length > 0) setEvaluations(loadedEvaluations);
          if (loadedRevReport) setRevenueReport(loadedRevReport);
          if (loadedMemReport) setMemberReport(loadedMemReport);
          setDashboard(loadedDashboard);
          setAuditLogs(loadedAuditLogs);
        } else if (currentRole === 'coach') {
          const [loadedClasses, loadedRooms, loadedMembers, loadedSessions, loadedPlans, loadedAttendances, loadedEvaluations, loadedResults] = await Promise.all([
            managerApi.getClasses({ subjects: [], coaches: [] }).catch(() => []),
            managerApi.getRooms().catch(() => []),
            managerApi.getMembers().catch(() => []),
            managerApi.getSessions({ coachId: authUser.userId }).catch(() => []),
            managerApi.getTrainingPlans({ coachId: authUser.userId }).catch(() => []),
            managerApi.getAttendances().catch(() => []),
            managerApi.getEvaluations({ coachId: authUser.userId }).catch(() => []),
            managerApi.getTrainingResults({ coachId: authUser.userId }).catch(() => []),
          ]);
          if (!active) return;
          setClasses(loadedClasses);
          setRooms(loadedRooms);
          setMembers(loadedMembers);
          setSessions(loadedSessions);
          if (loadedPlans.length > 0) setTrainingPlans(loadedPlans);
          if (loadedAttendances.length > 0) setAttendance(loadedAttendances);
          if (loadedEvaluations.length > 0) setEvaluations(loadedEvaluations);
          if (loadedResults.length > 0) setTrainingResults(loadedResults);
        } else if (currentRole === 'receptionist') {
          const [loadedMembers, loadedPackages, loadedClasses, loadedRooms, loadedSessions, loadedEnrollments, loadedInvoices, loadedAttendances] = await Promise.all([
            managerApi.getMembers(),
            managerApi.getPackages(),
            managerApi.getClasses({ subjects: [], coaches: [] }),
            managerApi.getRooms(),
            managerApi.getSessions().catch(() => []),
            managerApi.getEnrollments().catch(() => []),
            managerApi.getInvoices().catch(() => []),
            managerApi.getAttendances().catch(() => []),
          ]);
          if (!active) return;
          setMembers(loadedMembers);
          setPackages(loadedPackages);
          setClasses(loadedClasses);
          setRooms(loadedRooms);
          setSessions(loadedSessions);
          setEnrollments(loadedEnrollments);
          if (loadedInvoices.length > 0) setPayments(loadedInvoices);
          if (loadedAttendances.length > 0) setAttendance(loadedAttendances);
        } else if (currentRole === 'member') {
          const [loadedClasses, loadedPackages, loadedSessions, loadedMemberEnrollments, loadedInvoices, loadedAttendances, loadedPlans, loadedEvaluations] = await Promise.all([
            managerApi.getClasses({ subjects: [], coaches: [] }).catch(() => []),
            managerApi.getPackages().catch(() => []),
            managerApi.getSessions().catch(() => []),
            managerApi.getMemberEnrollments(authUser.userId).catch(() => []),
            managerApi.getInvoices({ memberId: authUser.userId }).catch(() => []),
            managerApi.getAttendances({ memberId: authUser.userId }).catch(() => []),
            managerApi.getTrainingPlans({ memberId: authUser.userId }).catch(() => []),
            managerApi.getEvaluations({ memberId: authUser.userId }).catch(() => []),
          ]);
          if (!active) return;
          setClasses(loadedClasses);
          setPackages(loadedPackages);
          setSessions(loadedSessions);
          setEnrollments(loadedMemberEnrollments);
          if (loadedInvoices.length > 0) setPayments(loadedInvoices);
          if (loadedAttendances.length > 0) setAttendance(loadedAttendances);
          if (loadedPlans.length > 0) setTrainingPlans(loadedPlans);
          if (loadedEvaluations.length > 0) setEvaluations(loadedEvaluations);
        }
      } catch (error) {
        if (!active) return;
        const message = getErrorMessage(error);
        setDataError(message);
        showToast(message, 'error');
      } finally {
        if (active) setDataLoading(false);
      }
    })();

    return () => { active = false; };
  }, [authUser]);

  const login = async (email, password) => {
    if (!API_ENABLED) {
      setLocalAuthenticated(true);
      return fallbackUser;
    }
    const user = await authApi.login(email, password);
    setAuthUser(user);
    setRole(roleFromApi(user.role));
    return user;
  };

  const logout = async () => {
    if (API_ENABLED) {
      try {
        await authApi.logout();
      } finally {
        setAuthUser(null);
      }
    } else {
      setLocalAuthenticated(false);
    }
  };

  const t = (key) => translations[language][key] || translations['vi'][key] || key;

  const addAuditLog = (action, target, previousValue, newValue, reason) => {
    const now = new Date();
    const timestamp = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`;
    const newLog = {
      id: `log-${Date.now()}`,
      timestamp,
      actorId: currentUser.id,
      actorName: `${currentUser.name} (${t(role === 'admin' ? 'roleAdmin' : role === 'manager' ? 'roleManager' : role === 'coach' ? 'roleCoach' : role === 'receptionist' ? 'roleReceptionist' : 'roleMember')})`,
      actorRole: role,
      action,
      target,
      previousValue,
      newValue,
      reason
    };
    setAuditLogs(prev => [newLog, ...prev]);
  };

  // User Management Actions
  const addUser = async (userData) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createUser(userData);
        setUsers(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Tạo người dùng ${created.name} thành công!` : `Created user ${created.name}!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newId = Date.now();
    const roleObj = roles.find(r => r.id === userData.roleId || r.name.toLowerCase() === (userData.roleName || '').toLowerCase());
    const newUser = {
      id: newId,
      code: `USR-${Math.floor(1000 + Math.random() * 9000)}`,
      fullName: userData.fullName || userData.name,
      name: userData.fullName || userData.name,
      email: userData.email,
      phone: userData.phone || '',
      roleId: roleObj?.id || 5,
      roleName: roleObj?.name || 'Member',
      role: roleObj?.name ? roleObj.name.toLowerCase() : 'member',
      status: 'active',
      rawStatus: 'Active',
      permissions: roleObj ? roleObj.permissions.map(p => p.name) : []
    };
    setUsers(prev => [newUser, ...prev]);
    showToast(language === 'vi' ? `Tạo người dùng ${newUser.name} thành công!` : `Created user ${newUser.name}!`);
    return newUser;
  };

  const updateUser = async (id, userData) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateUser(id, userData);
        setUsers(prev => prev.map(u => u.id === id ? updated : u));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Cập nhật người dùng thành công!` : `Updated user successfully!`);
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    setUsers(prev => prev.map(u => u.id === id ? { ...u, ...userData } : u));
    showToast(language === 'vi' ? 'Cập nhật thông tin thành công!' : 'Updated user!');
  };

  const updateUserStatus = async (id, status) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateUserStatus(id, status);
        setUsers(prev => prev.map(u => u.id === id ? updated : u));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Cập nhật trạng thái thành công!` : `Updated user status!`);
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    setUsers(prev => prev.map(u => u.id === id ? { ...u, status, rawStatus: status === 'suspended' ? 'Locked' : 'Active' } : u));
    showToast(language === 'vi' ? 'Đã đổi trạng thái tài khoản!' : 'Updated account status!');
  };

  const updateUserRole = async (id, roleId) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateUserRole(id, roleId);
        setUsers(prev => prev.map(u => u.id === id ? updated : u));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã thay đổi vai trò người dùng!` : `Changed user role!`);
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const roleObj = roles.find(r => r.id === roleId);
    setUsers(prev => prev.map(u => u.id === id ? {
      ...u,
      roleId,
      roleName: roleObj?.name,
      role: roleObj?.name.toLowerCase(),
      permissions: roleObj ? roleObj.permissions.map(p => p.name) : u.permissions
    } : u));
    showToast(language === 'vi' ? 'Đã đổi vai trò!' : 'Role changed!');
  };

  // RBAC Permission Assignment
  const updateRolePermissions = async (roleId, permissionIds) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateRolePermissions(roleId, permissionIds);
        setRoles(prev => prev.map(r => r.id === roleId ? updated : r));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(t('permissionsUpdated'));
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const perms = permissions.filter(p => permissionIds.includes(p.id));
    setRoles(prev => prev.map(r => r.id === roleId ? { ...r, permissions: perms } : r));
    showToast(t('permissionsUpdated'));
  };

  // Member management & Counter Registration
  const addMember = async (data) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createMember(data);
        setMembers(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã tạo tài khoản hội viên ${created.name}!` : `Created member profile ${created.name}!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newId = `mem-${Date.now()}`;
    const newCode = `MB-${Math.floor(1000 + Math.random() * 9000)}`;
    const pkg = data.currentPackageId || data.packageId ? packages.find(p => p.id === (data.currentPackageId || data.packageId)) : null;
    const newMember = {
      id: newId,
      code: newCode,
      name: data.name || 'Thành viên mới',
      email: data.email || '',
      phone: data.phone || '',
      joinDate: new Date().toISOString().split('T')[0],
      membershipStatus: pkg ? 'active' : 'none',
      currentPackageId: pkg ? pkg.id : null,
      currentPackageName: pkg ? (language === 'vi' ? pkg.nameVi : pkg.name) : (language === 'vi' ? 'Chưa có gói' : 'No Package'),
      totalSpent: pkg ? pkg.price : 0
    };
    setMembers(prev => [newMember, ...prev]);
    showToast(language === 'vi' ? `Đã tạo tài khoản hội viên ${newMember.name}!` : `Created member profile ${newMember.name}!`);
    return newMember;
  };

  const updateMember = (id, data) => {
    setMembers(prev => prev.map(m => m.id === id ? { ...m, ...data } : m));
  };

  // Subscription & Renewal Actions
  const subscribeMemberPackage = async (memberId, data) => {
    if (API_ENABLED) {
      try {
        const sub = await managerApi.subscribeMemberPackage(memberId, data);
        setMembers(prev => prev.map(m => String(m.id) === String(memberId) ? {
          ...m,
          currentPackageId: sub.packageId,
          currentPackageName: sub.packageName,
          membershipStatus: 'active'
        } : m));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? 'Đã đăng ký gói tập thành công!' : 'Package registered successfully!');
        return sub;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const member = members.find(m => String(m.id) === String(memberId));
    const packageId = typeof data === 'object' ? data.packageId : data;
    const pkg = packages.find(p => p.id === packageId);
    if (!member || !pkg) return;

    setMembers(prev => prev.map(m => String(m.id) === String(memberId) ? {
      ...m,
      currentPackageId: packageId,
      currentPackageName: language === 'vi' ? pkg.nameVi : pkg.name,
      membershipStatus: 'active',
      totalSpent: (m.totalSpent || 0) + (pkg.price || 0)
    } : m));
    showToast(language === 'vi' ? 'Đã đăng ký gói tập thành công!' : 'Package registered successfully!');
  };

  const renewMemberSubscription = async (memberId, data) => {
    if (API_ENABLED) {
      try {
        const sub = await managerApi.renewMemberSubscription(memberId, data);
        setMembers(prev => prev.map(m => String(m.id) === String(memberId) ? {
          ...m,
          currentPackageId: sub.packageId,
          currentPackageName: sub.packageName,
          membershipStatus: 'active'
        } : m));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? 'Gia hạn gói tập thành công!' : 'Subscription renewed successfully!');
        return sub;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const member = members.find(m => String(m.id) === String(memberId));
    const packageId = typeof data === 'object' ? data.packageId : data;
    const pkg = packages.find(p => p.id === packageId);
    if (!member || !pkg) return;

    setMembers(prev => prev.map(m => String(m.id) === String(memberId) ? {
      ...m,
      currentPackageId: packageId,
      currentPackageName: language === 'vi' ? pkg.nameVi : pkg.name,
      membershipStatus: 'active',
      totalSpent: (m.totalSpent || 0) + (pkg.price || 0)
    } : m));
    showToast(language === 'vi' ? 'Gia hạn gói tập thành công!' : 'Subscription renewed successfully!');
  };

  // Package Management Actions
  const createPackage = async (data) => {
    if (!API_ENABLED) {
      const newPkg = { ...data, id: `pkg-${Date.now()}` };
      setPackages(prev => [newPkg, ...prev]);
      return newPkg;
    }
    const created = await managerApi.createPackage(data);
    setPackages(prev => [created, ...prev]);
    return created;
  };

  const updatePackage = async (id, data) => {
    if (!API_ENABLED) {
      const updated = { ...packages.find(item => item.id === id), ...data };
      setPackages(prev => prev.map(item => item.id === id ? updated : item));
      return updated;
    }
    const updated = await managerApi.updatePackage(id, data);
    setPackages(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };

  const updatePackageStatus = async (id, status) => {
    if (!API_ENABLED) {
      const updated = { ...packages.find(item => item.id === id), status };
      setPackages(prev => prev.map(item => item.id === id ? updated : item));
      return updated;
    }
    const updated = await managerApi.updatePackageStatus(id, status);
    setPackages(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };

  // Staff Management Actions
  const addStaff = async (staffData) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createStaff(staffData);
        setStaff(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã tạo tài khoản nhân viên ${created.name}!` : `Created staff account for ${created.name}!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newId = `stf-${Date.now()}`;
    const prefix = staffData.role === 'coach' ? 'COA' : 'REC';
    const newStaff = {
      id: newId,
      code: `${prefix}-${Math.floor(100 + Math.random() * 900)}`,
      name: staffData.name,
      email: staffData.email,
      phone: staffData.phone,
      role: staffData.role,
      specialty: staffData.specialty || null,
      status: 'active',
      activeClassesCount: staffData.role === 'coach' ? 0 : undefined
    };
    setStaff(prev => [newStaff, ...prev]);
    showToast(language === 'vi' ? `Đã tạo tài khoản nhân viên ${newStaff.name}!` : `Created staff account for ${newStaff.name}!`);
    return newStaff;
  };

  const updateStaffStatus = async (id, status) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateStaffStatus(id, status);
        setStaff(prev => prev.map(s => s.id === id ? updated : s));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? 'Đã cập nhật trạng thái nhân viên!' : 'Updated staff status!');
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    setStaff(prev => prev.map(s => s.id === id ? { ...s, status } : s));
    showToast(language === 'vi' ? 'Đã cập nhật trạng thái nhân viên!' : 'Updated staff status!');
  };

  // Subject & Room Actions
  const createSubject = async (data) => {
    const created = await managerApi.createSubject(data);
    setSubjects(prev => [created, ...prev]);
    return created;
  };
  const updateSubject = async (id, data) => {
    const updated = await managerApi.updateSubject(id, data);
    setSubjects(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };
  const deleteSubject = async (id) => {
    await managerApi.deleteSubject(id);
    setSubjects(prev => prev.filter(item => item.id !== id));
  };

  const createRoom = async (data) => {
    const created = await managerApi.createRoom(data);
    setRooms(prev => [created, ...prev]);
    return created;
  };
  const updateRoom = async (id, data) => {
    const updated = await managerApi.updateRoom(id, data);
    setRooms(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };
  const updateRoomStatus = async (id, status) => {
    const updated = await managerApi.updateRoomStatus(id, status);
    setRooms(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };

  // Class Actions
  const createClass = async (data) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createClass(data, { subjects, coaches: staff });
        setClasses(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã tạo lớp ${created.name} thành công!` : `Created class ${created.name}!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    const newId = `cls-${Date.now()}`;
    const newClass = { ...data, id: newId, enrolledCount: 0 };
    setClasses(prev => [newClass, ...prev]);
    return newClass;
  };

  const updateClass = async (id, data) => {
    const updated = await managerApi.updateClass(id, data, { subjects, coaches: staff });
    setClasses(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };

  const assignCoach = async (id, coachId) => {
    const updated = await managerApi.assignCoach(id, coachId, { subjects, coaches: staff });
    setClasses(prev => prev.map(item => item.id === id ? updated : item));
    return updated;
  };

  // Session & Schedule Actions
  const createSession = async (data) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createSession(data);
        setSessions(prev => [created, ...prev]);
        showToast(language === 'vi' ? 'Đã tạo buổi học thành công!' : 'Created session successfully!');
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    const newSession = { ...data, id: Date.now(), status: 'scheduled', rawStatus: 'Scheduled' };
    setSessions(prev => [newSession, ...prev]);
    showToast(language === 'vi' ? 'Đã tạo buổi học thành công!' : 'Created session!');
    return newSession;
  };

  const generateSessions = async (data) => {
    if (API_ENABLED) {
      try {
        const list = await managerApi.generateSessions(data);
        setSessions(prev => [...list, ...prev]);
        showToast(language === 'vi' ? `Đã tạo tự động ${list.length} buổi học!` : `Generated ${list.length} sessions!`);
        return list;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    showToast(language === 'vi' ? 'Đã tạo lịch học định kỳ!' : 'Generated sessions!');
  };

  const updateSession = async (id, data) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateSession(id, data);
        setSessions(prev => prev.map(s => s.id === id ? updated : s));
        showToast(language === 'vi' ? 'Đã cập nhật buổi học!' : 'Updated session!');
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    setSessions(prev => prev.map(s => s.id === id ? { ...s, ...data } : s));
    showToast(language === 'vi' ? 'Đã cập nhật buổi học!' : 'Updated session!');
  };

  const updateSessionStatus = async (id, status) => {
    if (API_ENABLED) {
      try {
        const updated = await managerApi.updateSessionStatus(id, status);
        setSessions(prev => prev.map(s => s.id === id ? updated : s));
        showToast(language === 'vi' ? 'Đã cập nhật trạng thái buổi học!' : 'Updated session status!');
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    setSessions(prev => prev.map(s => s.id === id ? { ...s, status } : s));
  };

  const deleteSession = async (id) => {
    if (API_ENABLED) {
      try {
        await managerApi.deleteSession(id);
        setSessions(prev => prev.filter(s => s.id !== id));
        showToast(language === 'vi' ? 'Đã xóa buổi học!' : 'Deleted session!');
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    } else {
      setSessions(prev => prev.filter(s => s.id !== id));
      showToast(language === 'vi' ? 'Đã xóa buổi học!' : 'Deleted session!');
    }
  };

  const checkConflict = async (data) => {
    if (API_ENABLED) {
      return await managerApi.checkConflict(data);
    }
    return { hasConflict: false, message: 'OK', conflictDetails: [] };
  };

  // Class Booking & Enrollment Actions
  const bookClass = async (classId, memberId, className) => {
    const targetMemberId = memberId || currentUser.id;
    if (API_ENABLED) {
      try {
        const enrolled = await managerApi.enrollClass(classId, targetMemberId);
        setEnrollments(prev => [enrolled, ...prev]);
        // Refresh classes to update enrolledCount
        managerApi.getClasses({ subjects, coaches: staff }).then(setClasses).catch(() => {});
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã đăng ký lớp ${enrolled.className || className || ''} thành công!` : `Enrolled in class successfully!`);
        return enrolled;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newBooking = {
      id: `bk-${Date.now()}`,
      classId,
      className: className || 'Lớp học',
      memberId: targetMemberId,
      memberName: currentUser.name,
      bookingDate: new Date().toISOString().replace('T', ' ').substring(0, 16),
      status: 'confirmed'
    };
    setBookings(prev => [newBooking, ...prev]);
    setClasses(prev => prev.map(c => c.id === classId ? { ...c, enrolledCount: (c.enrolledCount || 0) + 1 } : c));
    showToast(language === 'vi' ? 'Đăng ký lớp thành công!' : 'Class booked successfully!');
  };

  const cancelClassBooking = async (classId, memberId) => {
    const targetMemberId = memberId || currentUser.id;
    if (API_ENABLED) {
      try {
        const cancelled = await managerApi.cancelEnrollment(classId, targetMemberId);
        setEnrollments(prev => prev.map(e => (e.classId === classId && e.memberId === targetMemberId) ? cancelled : e));
        managerApi.getClasses({ subjects, coaches: staff }).then(setClasses).catch(() => {});
        showToast(language === 'vi' ? 'Đã hủy đăng ký lớp thành công!' : 'Cancelled enrollment successfully!');
        return cancelled;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    setBookings(prev => prev.filter(b => !(b.classId === classId && b.memberId === targetMemberId)));
    setClasses(prev => prev.map(c => c.id === classId ? { ...c, enrolledCount: Math.max(0, (c.enrolledCount || 0) - 1) } : c));
    showToast(language === 'vi' ? 'Đã hủy đăng ký lớp!' : 'Booking cancelled!');
  };

  // Phase 4: Invoice & Payment Actions
  const recordPayment = async (memberId, packageId, amount, method, notes) => {
    if (API_ENABLED) {
      try {
        const payload = {
          memberId: Number(String(memberId).replace(/\D/g, '')) || Number(memberId),
          packageId: packageId ? (Number(String(packageId).replace(/\D/g, '')) || Number(packageId)) : null,
          amount: Number(amount),
          paymentMethod: method || 'Cash',
          paymentStatus: 'Paid',
          notes: notes || null,
        };
        const createdInvoice = await managerApi.createInvoice(payload);
        setPayments(prev => [createdInvoice, ...prev]);

        // If this payment is for a package, also update member's package if needed
        if (packageId) {
          managerApi.getMembers().then(setMembers).catch(() => {});
        }
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Ghi nhận thanh toán ${createdInvoice.code} thành công!` : `Payment recorded successfully!`);
        return createdInvoice;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newPayment = {
      id: `pay-${Date.now()}`,
      code: `TXN-${Math.floor(10000 + Math.random() * 90000)}`,
      transactionNo: `TXN-${Math.floor(10000 + Math.random() * 90000)}`,
      memberId,
      memberName: members.find(m => String(m.id) === String(memberId))?.name || 'Học viên',
      packageId,
      packageName: packages.find(p => String(p.id) === String(packageId))?.nameVi || 'Gói dịch vụ',
      amount,
      method,
      paymentDate: new Date().toISOString().replace('T', ' ').substring(0, 16),
      createdAt: new Date().toISOString().replace('T', ' ').substring(0, 16),
      status: 'successful',
      processedBy: currentUser.name
    };
    setPayments(prev => [newPayment, ...prev]);
    showToast(language === 'vi' ? 'Ghi nhận thanh toán thành công!' : 'Payment recorded!');
    return newPayment;
  };

  const payInvoice = async (invoiceId, paymentData) => {
    if (API_ENABLED) {
      try {
        const paid = await managerApi.payInvoice(invoiceId, paymentData);
        setPayments(prev => prev.map(p => p.id === invoiceId ? paid : p));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? 'Thanh toán hóa đơn thành công!' : 'Invoice paid successfully!');
        return paid;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    setPayments(prev => prev.map(p => p.id === invoiceId ? { ...p, status: 'successful', paymentStatus: 'Paid' } : p));
    showToast(language === 'vi' ? 'Thanh toán hóa đơn thành công!' : 'Invoice paid successfully!');
  };

  // Phase 5: Training Plan, Result, Attendance, Evaluation Actions
  const recordAttendance = async (classId, memberId, status, reason) => {
    const normStatus = status === 'present' ? 'Present' : status === 'late' ? 'Late' : 'Absent';
    if (API_ENABLED) {
      try {
        // Find matching session for this class
        const targetSession = sessions.find(s => s.classId === classId || s.id === classId) || sessions[0];
        const sessionId = targetSession?.id || 1;

        const result = await managerApi.recordTrainingResult({
          sessionId,
          memberId: Number(memberId),
          coachId: currentUser.role === 'coach' ? currentUser.id : (targetSession?.coachId || 3),
          content: reason || '',
          attendanceStatus: normStatus,
        });

        // Update local attendance state
        setAttendance(prev => {
          const filtered = prev.filter(a => !(a.classId === classId && a.memberId === memberId));
          return [result, ...filtered];
        });
        showToast(language === 'vi' ? `Đã điểm danh: ${t(status)}` : `Marked attendance: ${status}`);
        return result;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }

    const newAtt = {
      id: `att-${Date.now()}`,
      classId,
      memberId,
      memberName: members.find(m => m.id === memberId)?.name || 'Học viên',
      status,
      markedAt: new Date().toISOString().replace('T', ' ').substring(0, 16),
      markedBy: currentUser.name,
      reason
    };
    setAttendance(prev => {
      const filtered = prev.filter(a => !(a.classId === classId && a.memberId === memberId));
      return [newAtt, ...filtered];
    });
    showToast(language === 'vi' ? `Đã điểm danh: ${t(status)}` : `Marked attendance: ${status}`);
  };

  const correctAttendance = async (attendanceId, status, reason) => {
    if (API_ENABLED) {
      try {
        const corrected = await managerApi.correctAttendance({
          attendanceId,
          status,
          reason,
        });
        setAttendance(prev => prev.map(a => (a.id === attendanceId || a.attendanceId === attendanceId) ? corrected : a));
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? 'Đã điều chỉnh điểm danh và ghi Audit Log!' : 'Attendance corrected!');
        return corrected;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    setAttendance(prev => prev.map(a => a.id === attendanceId ? { ...a, status, reason } : a));
    showToast(language === 'vi' ? 'Đã điều chỉnh điểm danh!' : 'Attendance corrected!');
  };

  const checkInMember = async (memberId) => {
    if (API_ENABLED) {
      try {
        const checkedIn = await managerApi.checkInMember(memberId, currentUser.id);
        setAttendance(prev => [checkedIn, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Check-in thành công cho ${checkedIn.memberName}!` : `Member checked in successfully!`);
        return checkedIn;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    const mem = members.find(m => String(m.id) === String(memberId));
    const newCheckIn = {
      id: `att-${Date.now()}`,
      memberId,
      memberName: mem?.name || 'Học viên',
      status: 'checked_in',
      state: 'CheckedIn',
      checkInTime: new Date().toISOString()
    };
    setAttendance(prev => [newCheckIn, ...prev]);
    showToast(language === 'vi' ? 'Check-in thành công!' : 'Member checked in!');
  };

  const checkOutMember = async (memberId) => {
    if (API_ENABLED) {
      try {
        const checkedOut = await managerApi.checkOutMember(memberId);
        setAttendance(prev => prev.map(a => (a.memberId === memberId && !a.checkOutTime) ? checkedOut : a));
        showToast(language === 'vi' ? 'Check-out thành công!' : 'Member checked out successfully!');
        return checkedOut;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    showToast(language === 'vi' ? 'Check-out thành công!' : 'Member checked out!');
  };

  const createTrainingPlan = async (planData) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createTrainingPlan({
          ...planData,
          coachId: planData.coachId || currentUser.id,
        });
        setTrainingPlans(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã tạo kế hoạch tập luyện '${created.title}'!` : `Created training plan '${created.title}'!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    const newPlan = { ...planData, id: Date.now(), planId: Date.now(), progressPercent: 0 };
    setTrainingPlans(prev => [newPlan, ...prev]);
    showToast(language === 'vi' ? 'Đã tạo kế hoạch tập luyện!' : 'Created training plan!');
    return newPlan;
  };

  const updateTrainingPlan = async (planData) => {
    if (API_ENABLED && planData.id) {
      try {
        const updated = await managerApi.updateTrainingPlan(planData.id, planData);
        setTrainingPlans(prev => prev.map(p => p.id === planData.id ? updated : p));
        showToast(language === 'vi' ? 'Đã cập nhật kế hoạch tập luyện!' : 'Updated training plan!');
        return updated;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    setTrainingPlans(prev => prev.map(p => p.id === planData.id ? { ...p, ...planData } : p));
    showToast(language === 'vi' ? 'Đã lưu đánh giá tiến độ!' : 'Saved progress!');
  };

  const createEvaluation = async (evalData) => {
    if (API_ENABLED) {
      try {
        const created = await managerApi.createEvaluation({
          ...evalData,
          coachId: evalData.coachId || currentUser.id,
        });
        setEvaluations(prev => [created, ...prev]);
        managerApi.getAuditLogs().then(setAuditLogs).catch(() => {});
        showToast(language === 'vi' ? `Đã lưu đánh giá tiến độ!` : `Saved evaluation!`);
        return created;
      } catch (error) {
        showToast(getErrorMessage(error), 'error');
        throw error;
      }
    }
    const newEval = { ...evalData, id: Date.now(), evaluationId: Date.now() };
    setEvaluations(prev => [newEval, ...prev]);
    showToast(language === 'vi' ? 'Đã lưu đánh giá tiến độ!' : 'Saved evaluation!');
    return newEval;
  };

  return (
    <SCMSContext.Provider
      value={{
        language,
        setLanguage,
        role,
        setRole,
        currentTab,
        setCurrentTab,
        currentUser,
        isAuthenticated,
        authLoading,
        login,
        logout,
        apiEnabled: API_ENABLED,
        managerDataLoading: dataLoading,
        managerDataError: dataError,
        dashboard,
        users,
        roles,
        permissions,
        members,
        staff,
        packages,
        classes,
        subjects,
        rooms,
        sessions,
        enrollments,
        bookings,
        attendance,
        payments,
        trainingPlans,
        evaluations,
        trainingResults,
        revenueReport,
        memberReport,
        auditLogs,
        toast,
        showToast,
        addUser,
        updateUser,
        updateUserStatus,
        updateUserRole,
        updateRolePermissions,
        addMember,
        updateMember,
        subscribeMemberPackage,
        renewMemberSubscription,
        addStaff,
        updateStaffStatus,
        createSubject,
        updateSubject,
        deleteSubject,
        createRoom,
        updateRoom,
        updateRoomStatus,
        createClass,
        updateClass,
        assignCoach,
        createSession,
        generateSessions,
        updateSession,
        updateSessionStatus,
        deleteSession,
        checkConflict,
        bookClass,
        cancelClassBooking,
        createPackage,
        updatePackage,
        updatePackageStatus,
        recordPayment,
        payInvoice,
        recordAttendance,
        correctAttendance,
        checkInMember,
        checkOutMember,
        createTrainingPlan,
        updateTrainingPlan,
        createEvaluation,
        t
      }}
    >
      {children}
    </SCMSContext.Provider>
  );
};

const fallbackContext = {
  language: 'vi',
  setLanguage: () => {},
  role: 'admin',
  setRole: () => {},
  currentTab: 'admin_users',
  setCurrentTab: () => {},
  currentUser: { id: 1, name: 'Hệ Thống Admin', email: 'admin@scms.com', role: 'admin' },
  isAuthenticated: false,
  authLoading: false,
  login: async () => {},
  logout: async () => {},
  apiEnabled: false,
  managerDataLoading: false,
  managerDataError: null,
  dashboard: null,
  users: [],
  roles: [],
  permissions: [],
  members: [],
  staff: [],
  packages: [],
  classes: [],
  subjects: [],
  rooms: [],
  sessions: [],
  enrollments: [],
  bookings: [],
  attendance: [],
  payments: [],
  trainingPlans: [],
  evaluations: [],
  trainingResults: [],
  revenueReport: null,
  memberReport: null,
  auditLogs: [],
  toast: null,
  showToast: () => {},
  addUser: async () => {},
  updateUser: async () => {},
  updateUserStatus: async () => {},
  updateUserRole: async () => {},
  updateRolePermissions: async () => {},
  addMember: async () => {},
  updateMember: () => {},
  subscribeMemberPackage: async () => {},
  renewMemberSubscription: async () => {},
  addStaff: async () => {},
  updateStaffStatus: async () => {},
  createSubject: async () => {},
  updateSubject: async () => {},
  deleteSubject: async () => {},
  createRoom: async () => {},
  updateRoom: async () => {},
  updateRoomStatus: async () => {},
  createClass: async () => {},
  updateClass: async () => {},
  assignCoach: async () => {},
  createSession: async () => {},
  generateSessions: async () => {},
  updateSession: async () => {},
  updateSessionStatus: async () => {},
  deleteSession: async () => {},
  checkConflict: async () => {},
  bookClass: async () => {},
  cancelClassBooking: async () => {},
  createPackage: async () => {},
  updatePackage: async () => {},
  updatePackageStatus: async () => {},
  recordPayment: async () => {},
  payInvoice: async () => {},
  recordAttendance: async () => {},
  correctAttendance: async () => {},
  checkInMember: async () => {},
  checkOutMember: async () => {},
  createTrainingPlan: async () => {},
  updateTrainingPlan: async () => {},
  createEvaluation: async () => {},
  t: (key) => key
};

export const useSCMS = () => {
  const context = useContext(SCMSContext);
  return context || fallbackContext;
};

