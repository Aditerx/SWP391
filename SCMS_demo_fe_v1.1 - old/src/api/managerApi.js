import { httpClient } from './httpClient.js';
import {
  mapAttendance,
  mapAuditLog,
  mapClass,
  mapDashboard,
  mapEnrollment,
  mapEvaluation,
  mapInvoice,
  mapMember,
  mapMemberPackage,
  mapPackage,
  mapPermission,
  mapRole,
  mapRoom,
  mapSession,
  mapStaff,
  mapSubject,
  mapSubscription,
  mapTrainingPlan,
  mapTrainingResult,
  mapUser,
  toAttendanceRequest,
  toClassRequest,
  toEnrollmentRequest,
  toEvaluationRequest,
  toInvoicePaymentRequest,
  toInvoiceRequest,
  toMemberRequest,
  toPackageRequest,
  toRolePermissionRequest,
  toSessionGenerateRequest,
  toSessionRequest,
  toStaffRequest,
  toSubscriptionRequest,
  toTrainingPlanRequest,
  toTrainingResultRequest,
  toUserRequest,
} from './mappers.js';

export const getSubjects = async () => (await httpClient.get('/api/subjects')).map(mapSubject);
export const createSubject = async (data) => mapSubject(await httpClient.post('/api/subjects', data));
export const updateSubject = async (id, data) => mapSubject(await httpClient.put(`/api/subjects/${id}`, data));
export const deleteSubject = (id) => httpClient.delete(`/api/subjects/${id}`);

export const getRooms = async () => (await httpClient.get('/api/rooms')).map(mapRoom);
export const createRoom = async (data) => mapRoom(await httpClient.post('/api/rooms', data));
export const updateRoom = async (id, data) => mapRoom(await httpClient.put(`/api/rooms/${id}`, data));
export const updateRoomStatus = async (id, status) => mapRoom(await httpClient.patch(`/api/rooms/${id}/status`, { status }));

export const getClasses = async (lookups) => (await httpClient.get('/api/classes')).map((item) => mapClass(item, lookups));
export const createClass = async (data, lookups) => mapClass(
  await httpClient.post('/api/classes', toClassRequest(data)),
  { ...lookups, fallback: data },
);
export const updateClass = async (id, data, lookups) => mapClass(
  await httpClient.put(`/api/classes/${id}`, toClassRequest(data)),
  { ...lookups, fallback: data },
);
export const assignCoach = async (id, coachId, lookups) => mapClass(
  await httpClient.patch(`/api/classes/${id}/coach`, { coachId: Number(String(coachId).replace(/\D/g, '')) }),
  lookups,
);

export const getPackages = async () => (await httpClient.get('/api/packages')).map(mapPackage);
export const createPackage = async (data) => mapPackage(await httpClient.post('/api/packages', toPackageRequest(data)));
export const updatePackage = async (id, data) => mapPackage(await httpClient.put(`/api/packages/${id}`, toPackageRequest(data)));
export const updatePackageStatus = async (id, status) => mapPackage(await httpClient.patch(`/api/packages/${id}/status`, { status }));

export const getStaff = async () => (await httpClient.get('/api/staff')).map(mapStaff);
export const createStaff = async (data) => mapStaff(await httpClient.post('/api/staff', toStaffRequest(data)));
export const updateStaff = async (id, data) => mapStaff(await httpClient.put(`/api/staff/${id}`, toStaffRequest(data)));
export const updateStaffStatus = async (id, status) => mapStaff(await httpClient.patch(`/api/staff/${id}/status`, { status: status === 'suspended' ? 'Locked' : 'Active' }));

export const getMembers = async () => (await httpClient.get('/api/members')).map(mapMember);
export const getMemberDetail = async (id) => mapMember(await httpClient.get(`/api/members/${id}`));
export const createMember = async (data) => mapMember(await httpClient.post('/api/members', toMemberRequest(data)));
export const updateMember = async (id, data) => mapMember(await httpClient.put(`/api/members/${id}`, toMemberRequest(data)));
export const updateMemberStatus = async (id, status) => mapMember(await httpClient.patch(`/api/members/${id}/status`, { status: status === 'suspended' ? 'Locked' : 'Active' }));

// Subscriptions APIs
export const getMemberPackages = async () => (await httpClient.get('/api/subscriptions')).map(mapSubscription);
export const getMemberSubscriptions = async (memberId) => (await httpClient.get(`/api/members/${memberId}/subscriptions`)).map(mapSubscription);
export const subscribeMemberPackage = async (memberId, data) => {
  const payload = typeof data === 'object' ? toSubscriptionRequest(data) : { packageId: data };
  return mapSubscription(await httpClient.post(`/api/members/${memberId}/subscriptions`, payload));
};
export const renewMemberSubscription = async (memberId, data) => {
  const payload = typeof data === 'object' ? { ...toSubscriptionRequest(data), isRenewal: true } : { packageId: data, isRenewal: true };
  return mapSubscription(await httpClient.post(`/api/members/${memberId}/subscriptions`, payload));
};
export const updateMemberPackageStatus = async (id, status) => mapSubscription(await httpClient.patch(`/api/subscriptions/${id}/status`, { status }));

// Users & RBAC APIs
export const getUsers = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.role && params.role !== 'all') query.set('role', params.role);
  if (params.search) query.set('search', params.search);
  const qs = query.toString();
  return (await httpClient.get(`/api/users${qs ? `?${qs}` : ''}`)).map(mapUser);
};
export const getUserById = async (id) => mapUser(await httpClient.get(`/api/users/${id}`));
export const createUser = async (data) => mapUser(await httpClient.post('/api/users', toUserRequest(data)));
export const updateUser = async (id, data) => mapUser(await httpClient.put(`/api/users/${id}`, toUserRequest(data)));
export const updateUserStatus = async (id, status) => mapUser(await httpClient.patch(`/api/users/${id}/status`, { status: status === 'suspended' ? 'Locked' : 'Active' }));
export const updateUserRole = async (id, roleId) => mapUser(await httpClient.patch(`/api/users/${id}/role`, { roleId: Number(roleId) }));

export const getRoles = async () => (await httpClient.get('/api/roles')).map(mapRole);
export const getRoleById = async (id) => mapRole(await httpClient.get(`/api/roles/${id}`));
export const getPermissions = async () => (await httpClient.get('/api/permissions')).map(mapPermission);
export const updateRolePermissions = async (roleId, permissionIds) => mapRole(
  await httpClient.put(`/api/roles/${roleId}/permissions`, toRolePermissionRequest(permissionIds))
);

// Session & Schedule APIs
export const getSessions = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.classId) query.set('classId', params.classId);
  if (params.coachId) query.set('coachId', params.coachId);
  if (params.roomId) query.set('roomId', params.roomId);
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.startDate) query.set('startDate', params.startDate);
  if (params.endDate) query.set('endDate', params.endDate);
  if (params.status) query.set('status', params.status);
  const qs = query.toString();
  return (await httpClient.get(`/api/sessions${qs ? `?${qs}` : ''}`)).map(mapSession);
};

export const getSessionById = async (id) => mapSession(await httpClient.get(`/api/sessions/${id}`));

export const createSession = async (data) => mapSession(await httpClient.post('/api/sessions', toSessionRequest(data)));

export const generateSessions = async (data) => (await httpClient.post('/api/sessions/generate', toSessionGenerateRequest(data))).map(mapSession);

export const checkConflict = async (data) => {
  return await httpClient.post('/api/sessions/check-conflict', {
    classId: data.classId ? Number(data.classId) : null,
    coachId: data.coachId ? Number(data.coachId) : null,
    roomId: data.roomId ? Number(data.roomId) : null,
    sessionDate: data.sessionDate || data.date,
    startTime: data.startTime ? (data.startTime.length === 5 ? `${data.startTime}:00` : data.startTime) : null,
    endTime: data.endTime ? (data.endTime.length === 5 ? `${data.endTime}:00` : data.endTime) : null,
    excludeSessionId: data.excludeSessionId ? Number(data.excludeSessionId) : null,
  });
};

export const updateSession = async (id, data) => mapSession(await httpClient.put(`/api/sessions/${id}`, toSessionRequest(data)));

export const updateSessionStatus = async (id, status) => mapSession(await httpClient.patch(`/api/sessions/${id}/status`, { status }));

export const deleteSession = (id) => httpClient.delete(`/api/sessions/${id}`);

// Enrollment & Booking APIs
export const getEnrollments = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.classId) query.set('classId', params.classId);
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.status) query.set('status', params.status);
  const qs = query.toString();
  return (await httpClient.get(`/api/enrollments${qs ? `?${qs}` : ''}`)).map(mapEnrollment);
};

export const getClassEnrollments = async (classId) => (await httpClient.get(`/api/classes/${classId}/enrollments`)).map(mapEnrollment);

export const getMemberEnrollments = async (memberId) => (await httpClient.get(`/api/members/${memberId}/enrollments`)).map(mapEnrollment);

export const enrollClass = async (classId, memberId) => {
  const payload = memberId ? { memberId: Number(memberId) } : {};
  return mapEnrollment(await httpClient.post(`/api/classes/${classId}/enroll`, payload));
};

export const cancelEnrollment = async (classId, memberId) => {
  const payload = memberId ? { memberId: Number(memberId) } : {};
  return mapEnrollment(await httpClient.post(`/api/classes/${classId}/cancel-enrollment`, payload));
};

export const cancelEnrollmentById = async (id) => mapEnrollment(await httpClient.patch(`/api/enrollments/${id}/cancel`));

export const getDashboard = async () => mapDashboard(await httpClient.get('/api/reports/dashboard'));
export const getAuditLogs = async () => (await httpClient.get('/api/audit-logs')).map(mapAuditLog);

// Phase 4: Invoices & Advanced Reports APIs
export const getInvoices = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.packageId) query.set('packageId', params.packageId);
  if (params.receptionistId) query.set('receptionistId', params.receptionistId);
  if (params.status && params.status !== 'all') query.set('status', params.status);
  if (params.method) query.set('method', params.method);
  if (params.startDate) query.set('startDate', params.startDate);
  if (params.endDate) query.set('endDate', params.endDate);
  const qs = query.toString();
  return (await httpClient.get(`/api/invoices${qs ? `?${qs}` : ''}`)).map(mapInvoice);
};

export const getInvoiceById = async (id) => mapInvoice(await httpClient.get(`/api/invoices/${id}`));

export const createInvoice = async (data) => mapInvoice(await httpClient.post('/api/invoices', toInvoiceRequest(data)));

export const payInvoice = async (id, data) => mapInvoice(await httpClient.post(`/api/invoices/${id}/pay`, toInvoicePaymentRequest(data)));

export const updateInvoiceStatus = async (id, status) => mapInvoice(await httpClient.patch(`/api/invoices/${id}/status`, { status }));

export const getRevenueReport = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.startDate) query.set('startDate', params.startDate);
  if (params.endDate) query.set('endDate', params.endDate);
  if (params.groupBy) query.set('groupBy', params.groupBy);
  const qs = query.toString();
  return await httpClient.get(`/api/reports/revenue${qs ? `?${qs}` : ''}`);
};

export const getMemberReport = async () => await httpClient.get('/api/reports/members');

// Phase 5: Training Plans, Results, Attendance & Evaluations APIs
export const getTrainingPlans = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.coachId) query.set('coachId', params.coachId);
  if (params.classId) query.set('classId', params.classId);
  if (params.memberId) query.set('memberId', params.memberId);
  const qs = query.toString();
  return (await httpClient.get(`/api/training-plans${qs ? `?${qs}` : ''}`)).map(mapTrainingPlan);
};

export const getTrainingPlanById = async (id) => mapTrainingPlan(await httpClient.get(`/api/training-plans/${id}`));

export const createTrainingPlan = async (data) => mapTrainingPlan(await httpClient.post('/api/training-plans', toTrainingPlanRequest(data)));

export const updateTrainingPlan = async (id, data) => mapTrainingPlan(await httpClient.put(`/api/training-plans/${id}`, toTrainingPlanRequest(data)));

export const deleteTrainingPlan = (id) => httpClient.delete(`/api/training-plans/${id}`);

export const getTrainingResults = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.sessionId) query.set('sessionId', params.sessionId);
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.coachId) query.set('coachId', params.coachId);
  const qs = query.toString();
  return (await httpClient.get(`/api/training-results${qs ? `?${qs}` : ''}`)).map(mapTrainingResult);
};

export const recordTrainingResult = async (data) => mapTrainingResult(await httpClient.post('/api/training-results', toTrainingResultRequest(data)));

export const recordBulkTrainingResults = async (data) => {
  const res = await httpClient.post('/api/training-results/bulk', {
    sessionId: Number(data.sessionId),
    coachId: data.coachId ? Number(data.coachId) : undefined,
    items: (data.items || []).map(item => ({
      memberId: Number(item.memberId),
      attendanceStatus: item.attendanceStatus || (item.status === 'late' ? 'Late' : item.status === 'absent' ? 'Absent' : 'Present'),
      content: item.content || item.reason || '',
    })),
  });
  return res.map(mapTrainingResult);
};

export const getAttendances = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.sessionId) query.set('sessionId', params.sessionId);
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.recordedBy) query.set('recordedBy', params.recordedBy);
  if (params.state) query.set('state', params.state);
  if (params.startDate) query.set('startDate', params.startDate);
  if (params.endDate) query.set('endDate', params.endDate);
  const qs = query.toString();
  return (await httpClient.get(`/api/attendances${qs ? `?${qs}` : ''}`)).map(mapAttendance);
};

export const checkInMember = async (memberId, recordedBy) => mapAttendance(
  await httpClient.post('/api/attendances/check-in', {
    memberId: Number(memberId),
    recordedBy: recordedBy ? Number(recordedBy) : undefined,
  })
);

export const checkOutMember = async (memberId) => mapAttendance(
  await httpClient.post(`/api/attendances/check-out?memberId=${Number(memberId)}`)
);

export const correctAttendance = async (data) => mapAttendance(
  await httpClient.post('/api/attendances/correct', {
    attendanceId: Number(data.attendanceId || data.id),
    newState: data.newState || (data.status === 'late' ? 'Late' : data.status === 'absent' ? 'Absent' : 'Present'),
    reason: data.reason || 'Manual correction',
  })
);

export const getEvaluations = async (params = {}) => {
  const query = new URLSearchParams();
  if (params.memberId) query.set('memberId', params.memberId);
  if (params.coachId) query.set('coachId', params.coachId);
  const qs = query.toString();
  return (await httpClient.get(`/api/evaluations${qs ? `?${qs}` : ''}`)).map(mapEvaluation);
};

export const getEvaluationById = async (id) => mapEvaluation(await httpClient.get(`/api/evaluations/${id}`));

export const createEvaluation = async (data) => mapEvaluation(await httpClient.post('/api/evaluations', toEvaluationRequest(data)));

export const updateEvaluation = async (id, data) => mapEvaluation(await httpClient.put(`/api/evaluations/${id}`, toEvaluationRequest(data)));

export const deleteEvaluation = (id) => httpClient.delete(`/api/evaluations/${id}`);

