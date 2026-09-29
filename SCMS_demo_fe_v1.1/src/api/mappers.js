const normalizeStatus = (status, fallback = null) => status
  ? String(status).trim().toLowerCase().replace(/\s+/g, '_')
  : fallback;

const asNumber = (value) => {
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const idNumber = (value) => {
  const direct = asNumber(value);
  if (direct !== null) return direct;
  const match = String(value ?? '').match(/(\d+)$/);
  return match ? Number(match[1]) : null;
};

export function parseBenefits(value) {
  if (Array.isArray(value)) return value.filter(Boolean);
  if (!value) return [];
  return String(value).split(/\s*(?:;|\r?\n)\s*/).filter(Boolean);
}

export function serializeBenefits(value) {
  return Array.isArray(value) ? value.filter(Boolean).join(';') : (value || '');
}

export const mapSubject = (subject) => ({
  id: subject.id,
  name: subject.name,
  description: subject.description ?? null,
});

export const mapRoom = (room) => ({
  id: room.id,
  name: room.name,
  location: room.location ?? null,
  capacity: asNumber(room.capacity),
  status: normalizeStatus(room.status, 'available'),
});

export function mapPackage(item) {
  const durationDays = asNumber(item.durationDays);
  const benefits = parseBenefits(item.benefits);
  return {
    id: item.id,
    name: item.name,
    nameVi: item.name,
    price: asNumber(item.price) ?? 0,
    durationDays,
    durationMonths: durationDays === null ? null : Math.round(durationDays / 30),
    description: item.benefits ?? null,
    descriptionVi: item.benefits ?? null,
    benefits,
    benefitsVi: benefits,
    maxClassesPerMonth: null,
    tier: null,
    status: normalizeStatus(item.status, 'active'),
  };
}

const classStatus = (status) => {
  const normalized = normalizeStatus(status, 'open');
  if (['open', 'ongoing', 'closed', 'cancelled'].includes(normalized)) return normalized;
  if (normalized === 'active' || normalized === 'published') return 'open';
  if (normalized === 'completed') return 'closed';
  return normalized;
};

export function mapClass(item, { subjects = [], coaches = [], fallback = {} } = {}) {
  const subject = subjects.find((entry) => String(entry.id) === String(item.subjectId));
  const coach = coaches.find((entry) => idNumber(entry.id) === asNumber(item.coachId));
  return {
    id: item.id,
    name: item.name,
    nameVi: item.name,
    subjectId: item.subjectId ?? null,
    coachId: item.coachId ?? null,
    coachName: coach?.name ?? fallback.coachName ?? null,
    capacity: asNumber(item.maxCapacity) ?? 0,
    status: classStatus(item.status),
    apiStatus: item.status ?? null,
    startDate: item.startDate ?? null,
    endDate: item.endDate ?? null,
    date: fallback.date ?? item.startDate ?? null,
    startTime: fallback.startTime ?? null,
    endTime: fallback.endTime ?? null,
    durationMinutes: fallback.durationMinutes ?? null,
    enrolledCount: asNumber(item.enrolledCount) ?? 0,
    code: null,
    category: subject?.name ?? fallback.category ?? null,
    room: fallback.room ?? null,
    roomVi: fallback.roomVi ?? fallback.room ?? null,
    minTierRequired: null,
  };
}

export function mapSession(item) {
  return {
    id: item.id,
    classId: item.classId,
    className: item.className,
    subjectId: item.subjectId,
    subjectName: item.subjectName,
    coachId: item.coachId,
    coachName: item.coachName,
    roomId: item.roomId,
    roomName: item.roomName,
    roomLocation: item.roomLocation,
    sessionDate: item.sessionDate,
    date: item.sessionDate,
    startTime: item.startTime ? item.startTime.substring(0, 5) : '',
    endTime: item.endTime ? item.endTime.substring(0, 5) : '',
    status: normalizeStatus(item.status, 'scheduled'),
    rawStatus: item.status,
    enrolledCount: asNumber(item.enrolledCount) ?? 0,
    maxCapacity: asNumber(item.maxCapacity) ?? 0,
  };
}

export function toSessionRequest(item) {
  return {
    classId: idNumber(item.classId),
    roomId: idNumber(item.roomId),
    sessionDate: item.sessionDate || item.date,
    startTime: item.startTime ? (item.startTime.length === 5 ? `${item.startTime}:00` : item.startTime) : null,
    endTime: item.endTime ? (item.endTime.length === 5 ? `${item.endTime}:00` : item.endTime) : null,
    status: item.status || 'Scheduled',
  };
}

export function toSessionGenerateRequest(item) {
  return {
    classId: idNumber(item.classId),
    roomId: idNumber(item.roomId),
    startDate: item.startDate,
    endDate: item.endDate,
    daysOfWeek: item.daysOfWeek.map(Number),
    startTime: item.startTime ? (item.startTime.length === 5 ? `${item.startTime}:00` : item.startTime) : null,
    endTime: item.endTime ? (item.endTime.length === 5 ? `${item.endTime}:00` : item.endTime) : null,
  };
}

export function mapEnrollment(item) {
  return {
    id: item.id,
    memberId: item.memberId,
    memberName: item.memberName,
    memberCode: item.memberCode,
    memberEmail: item.memberEmail,
    memberPhone: item.memberPhone,
    classId: item.classId,
    className: item.className,
    subjectName: item.subjectName,
    coachName: item.coachName,
    enrolledAt: item.enrolledAt,
    status: normalizeStatus(item.status, 'registered'),
    rawStatus: item.status,
  };
}

export function toEnrollmentRequest(item) {
  return {
    memberId: item.memberId ? idNumber(item.memberId) : undefined,
    classId: idNumber(item.classId),
  };
}

export function mapAuditLog(item) {
  const target = [item.targetEntity, item.targetId].filter((value) => value !== null && value !== undefined).join(' #');
  return {
    id: item.id,
    timestamp: item.createdAt ?? null,
    actorId: item.userId ?? null,
    actorName: item.userId ? `User #${item.userId}` : 'System',
    actorRole: null,
    action: item.action ?? 'UNKNOWN',
    target: target || 'N/A',
    previousValue: null,
    newValue: null,
    reason: item.detail ?? null,
  };
}

export const mapDashboard = (item) => ({
  totalUsers: asNumber(item.totalUsers) ?? 0,
  totalMembers: asNumber(item.totalMembers) ?? 0,
  totalClasses: asNumber(item.totalClasses) ?? 0,
  totalPackages: asNumber(item.totalPackages) ?? 0,
});

export const mapStaff = (item) => ({
  id: item.id,
  code: item.code || `STF-${item.id}`,
  name: item.name,
  email: item.email,
  phone: item.phone || '',
  role: item.role || 'coach',
  specialty: item.specialization || '',
  specialtyVi: item.specializationVi || item.specialization || '',
  status: normalizeStatus(item.status, 'active') === 'locked' ? 'suspended' : normalizeStatus(item.status, 'active'),
  activeClassesCount: asNumber(item.activeClassesCount) || 0,
});

export const mapMember = (item) => ({
  id: item.id,
  code: item.code || `MB-${1000 + item.id}`,
  name: item.name,
  email: item.email,
  phone: item.phone || '',
  joinDate: item.joinDate || null,
  currentPackageId: item.currentPackageId || null,
  currentPackageName: item.currentPackageName || 'Chưa đăng ký gói',
  membershipStatus: normalizeStatus(item.membershipStatus, 'active'),
  primaryCoachId: item.primaryCoachId || null,
  primaryCoachName: item.primaryCoachName || null,
  totalSpent: asNumber(item.totalSpent) || 0,
  goal: item.goal || null,
  healthNote: item.healthNote || null,
  status: normalizeStatus(item.status, 'active'),
});

export const mapMemberPackage = (item) => ({
  id: item.subscriptionId,
  memberId: item.memberId,
  memberName: item.memberName,
  packageId: item.packageId,
  packageName: item.packageName,
  price: asNumber(item.price) || 0,
  startDate: item.startDate,
  endDate: item.endDate,
  status: normalizeStatus(item.status, 'active'),
});

export const toClassRequest = (item) => {
  const statusMap = {
    open: 'Open',
    ongoing: 'Ongoing',
    closed: 'Closed',
    cancelled: 'Cancelled',
    published: 'Open',
    completed: 'Closed',
  };
  const normalizedStatus = item.status ? String(item.status).toLowerCase() : 'open';
  return {
    name: item.name || item.nameVi,
    subjectId: idNumber(item.subjectId),
    coachId: idNumber(item.coachId),
    maxCapacity: asNumber(item.maxCapacity ?? item.capacity),
    startDate: item.startDate ?? item.date ?? null,
    endDate: item.endDate ?? item.date ?? null,
    status: item.apiStatus || statusMap[normalizedStatus] || 'Open',
  };
};

export const toPackageRequest = (item) => {
  const durationDays = asNumber(item.durationDays);
  const durationMonths = asNumber(item.durationMonths);
  return {
    name: item.name,
    durationDays: durationDays ?? (durationMonths === null ? null : Math.round(durationMonths * 30)),
    price: asNumber(item.price),
    benefits: serializeBenefits(item.benefits),
    status: item.status,
  };
};

export const toStaffRequest = (item) => ({
  name: item.name,
  email: item.email,
  phone: item.phone,
  role: item.role === 'coach' ? 'Coach' : item.role === 'receptionist' ? 'Receptionist' : item.role === 'admin' ? 'Admin' : 'CenterManager',
  specialization: item.specialty || item.specialization,
  password: item.password || '12345678',
  status: item.status === 'suspended' ? 'Locked' : 'Active',
});

export const toMemberRequest = (item) => ({
  name: item.name,
  email: item.email,
  phone: item.phone,
  password: item.password || '12345678',
  packageId: idNumber(item.packageId || item.currentPackageId),
  goal: item.goal,
  healthNote: item.healthNote,
  address: item.address,
  gender: item.gender,
  dateOfBirth: item.dateOfBirth,
  status: item.status === 'suspended' ? 'Locked' : 'Active',
});

export const mapUser = (item) => ({
  id: item.id,
  code: item.code || `USR-${1000 + item.id}`,
  name: item.fullName,
  fullName: item.fullName,
  email: item.email,
  phone: item.phone || '',
  address: item.address || '',
  gender: item.gender || 'Other',
  dateOfBirth: item.dateOfBirth || null,
  roleId: item.roleId,
  role: item.roleName ? item.roleName.toLowerCase() : 'member',
  roleName: item.roleName || 'Member',
  status: normalizeStatus(item.status, 'active') === 'locked' ? 'suspended' : normalizeStatus(item.status, 'active'),
  rawStatus: item.status || 'Active',
  createdAt: item.createdAt,
  permissions: item.permissions || [],
});

export const toUserRequest = (item) => ({
  fullName: item.fullName || item.name,
  email: item.email,
  phone: item.phone,
  address: item.address,
  gender: item.gender,
  dateOfBirth: item.dateOfBirth,
  roleId: item.roleId ? Number(item.roleId) : undefined,
  roleName: item.roleName || (item.role === 'admin' ? 'Admin' : item.role === 'manager' ? 'CenterManager' : item.role === 'coach' ? 'Coach' : item.role === 'receptionist' ? 'Receptionist' : 'Member'),
  password: item.password || undefined,
  status: item.status === 'suspended' ? 'Locked' : (item.status ? (item.status.charAt(0).toUpperCase() + item.status.slice(1)) : 'Active'),
});

export const mapPermission = (item) => ({
  id: item.id,
  name: item.name,
  description: item.description,
});

export const mapRole = (item) => ({
  id: item.id,
  name: item.name,
  description: item.description,
  permissions: (item.permissions || []).map(mapPermission),
});

export const toRolePermissionRequest = (permissionIds) => ({
  permissionIds: permissionIds.map(Number),
});

export const mapSubscription = mapMemberPackage;

export const toSubscriptionRequest = (item) => ({
  packageId: idNumber(item.packageId),
  startDate: item.startDate || null,
  durationDays: asNumber(item.durationDays),
  isRenewal: Boolean(item.isRenewal),
  paymentMethod: item.paymentMethod || 'Cash',
  notes: item.notes || null,
});

// Phase 4: Invoice / Payment Mappers
export const mapInvoice = (item) => ({
  id: item.invoiceId,
  invoiceId: item.invoiceId,
  code: item.gatewayTransactionRef || `TXN-${70000 + item.invoiceId}`,
  transactionNo: item.gatewayTransactionRef || `TXN-${70000 + item.invoiceId}`,
  memberId: item.memberId,
  memberName: item.memberName || 'N/A',
  memberEmail: item.memberEmail,
  memberPhone: item.memberPhone,
  packageId: item.packageId ? `pkg-${item.packageId}` : null,
  rawPackageId: item.packageId,
  packageName: item.packageName || 'Gói tập Gym & Fitness',
  durationDays: item.durationDays,
  receptionistId: item.receptionistId,
  processedBy: item.receptionistName || 'Lễ tân Fitzone',
  amount: asNumber(item.amount) ?? 0,
  method: (item.paymentMethod || 'Cash').toLowerCase().replace('transfer', '_transfer'),
  paymentMethod: item.paymentMethod || 'Cash',
  status: item.paymentStatus === 'Paid' ? 'successful' : (item.paymentStatus === 'Failed' ? 'failed' : 'pending'),
  paymentStatus: item.paymentStatus || 'Pending',
  paymentDate: item.paymentDate,
  createdAt: item.paymentDate ? item.paymentDate.replace('T', ' ').substring(0, 16) : '2026-09-28 09:00',
  gatewayTransactionRef: item.gatewayTransactionRef,
});

export const toInvoiceRequest = (item) => ({
  memberId: idNumber(item.memberId),
  packageId: idNumber(item.packageId || item.rawPackageId),
  receptionistId: idNumber(item.receptionistId),
  amount: asNumber(item.amount),
  paymentMethod: item.paymentMethod || (item.method === 'bank_transfer' ? 'BankTransfer' : item.method === 'card_pos' ? 'CreditCard' : 'Cash'),
  paymentStatus: item.paymentStatus || (item.status === 'successful' ? 'Paid' : 'Pending'),
  gatewayTransactionRef: item.gatewayTransactionRef || item.transactionNo || null,
  notes: item.notes || null,
});

export const toInvoicePaymentRequest = (item) => ({
  paymentMethod: item.paymentMethod || (item.method === 'bank_transfer' ? 'BankTransfer' : item.method === 'card_pos' ? 'CreditCard' : 'Cash'),
  gatewayTransactionRef: item.gatewayTransactionRef || item.transactionNo || null,
  receptionistId: idNumber(item.receptionistId),
  notes: item.notes || null,
});

// Phase 5: Training Plan, Result, Attendance, Evaluation Mappers
export const mapTrainingPlan = (item) => ({
  id: item.planId,
  planId: item.planId,
  coachId: item.coachId,
  coachName: item.coachName || 'Huấn luyện viên',
  classId: item.classId,
  className: item.className,
  memberId: item.memberId,
  memberName: item.memberName || 'Học viên',
  title: item.title,
  content: item.content || '',
  goal: item.goal || 'Tăng cường thể lực và dẻo dai',
  goalVi: item.goal || 'Tăng cường thể lực và dẻo dai',
  startDate: item.startDate,
  endDate: item.endDate,
  createdAt: item.createdAt,
  progressPercent: 75,
  exercises: [
    { name: 'Khởi động xoay khớp', sets: 1, reps: '5-10 phút', weight: 'Bodyweight' },
    { name: 'Tư thế Chiến binh (Warrior Pose)', sets: 3, reps: '45 giây', weight: 'Bodyweight' },
    { name: 'Giãn cơ lưng & đùi sau', sets: 3, reps: '1 phút', weight: 'Thảm tập' }
  ],
  resultNotes: item.content || 'Học viên tiến bộ tốt, giữ vững form chuẩn.'
});

export const toTrainingPlanRequest = (item) => ({
  coachId: idNumber(item.coachId),
  classId: idNumber(item.classId),
  memberId: idNumber(item.memberId),
  title: item.title,
  content: item.content || item.resultNotes,
  goal: item.goal || item.goalVi,
  startDate: item.startDate || null,
  endDate: item.endDate || null,
});

export const mapTrainingResult = (item) => ({
  id: item.resultId,
  resultId: item.resultId,
  sessionId: item.sessionId,
  sessionDate: item.sessionDate,
  classId: item.classId,
  className: item.className,
  memberId: item.memberId,
  memberName: item.memberName,
  coachId: item.coachId,
  coachName: item.coachName,
  content: item.content || '',
  attendanceStatus: item.attendanceStatus,
  status: normalizeStatus(item.attendanceStatus, 'present'),
  recordedAt: item.recordedAt,
});

export const toTrainingResultRequest = (item) => ({
  sessionId: idNumber(item.sessionId),
  memberId: idNumber(item.memberId),
  coachId: idNumber(item.coachId),
  content: item.content || '',
  attendanceStatus: item.attendanceStatus || (item.status === 'late' ? 'Late' : item.status === 'absent' ? 'Absent' : 'Present'),
});

export const mapAttendance = (item) => ({
  id: item.attendanceId,
  attendanceId: item.attendanceId,
  sessionId: item.sessionId,
  sessionDate: item.sessionDate,
  classId: item.classId,
  className: item.className,
  memberId: item.memberId,
  memberName: item.memberName,
  memberCode: item.memberCode,
  recordedBy: item.recordedBy,
  recordedByName: item.recordedByName,
  state: item.state,
  status: normalizeStatus(item.state, 'present'),
  checkInTime: item.checkInTime,
  checkOutTime: item.checkOutTime,
});

export const toAttendanceRequest = (item) => ({
  sessionId: idNumber(item.sessionId),
  memberId: idNumber(item.memberId),
  recordedBy: idNumber(item.recordedBy),
  state: item.state || (item.status === 'late' ? 'Late' : item.status === 'absent' ? 'Absent' : 'Present'),
  checkInTime: item.checkInTime || null,
  checkOutTime: item.checkOutTime || null,
});

export const mapEvaluation = (item) => ({
  id: item.evaluationId,
  evaluationId: item.evaluationId,
  memberId: item.memberId,
  memberName: item.memberName,
  memberCode: item.memberCode,
  coachId: item.coachId,
  coachName: item.coachName,
  evaluationDate: item.evaluationDate,
  comment: item.comment,
  progressScore: asNumber(item.progressScore) || 0,
});

export const toEvaluationRequest = (item) => ({
  memberId: idNumber(item.memberId),
  coachId: idNumber(item.coachId),
  evaluationDate: item.evaluationDate || null,
  comment: item.comment,
  progressScore: asNumber(item.progressScore),
});

