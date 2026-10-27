import { httpClient } from './httpClient.js';

// Current backend contracts. Prices and state transitions are decided by the backend.
const query = (values = {}) => {
  const params = new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v !== null && v !== undefined));
  return params.size ? `?${params}` : '';
};
const post = (path, data) => httpClient.post(path, data, { notifyForbidden: false });
export const demoApi = {
  me: () => httpClient.get('/api/auth/me'),
  packages: () => httpClient.get('/api/packages'),
  members: () => httpClient.get('/api/members'),
  classes: () => httpClient.get('/api/classes'),
  staff: () => httpClient.get('/api/staff'),
  saveClass: (data, id) => id ? httpClient.put(`/api/classes/${id}`, data, { notifyForbidden: false }) : post('/api/classes', data),
  subjects: () => httpClient.get('/api/subjects'),
  subscriptions: (memberId) => httpClient.get(`/api/members/${memberId}/subscriptions`),
  subscribe: (packageId, memberId) => post(memberId ? `/api/members/${memberId}/subscriptions` : '/api/members/me/subscriptions', { packageId: Number(packageId), paymentMethod: 'Cash' }),
  invoices: (own, filters) => httpClient.get(own ? '/api/members/me/invoices' : `/api/invoices${query(filters)}`),
  confirmCash: (id) => post(`/api/invoices/${id}/confirm-cash`),
  cancelInvoice: (id) => post(`/api/invoices/${id}/cancel`),
  enrollments: (values) => httpClient.get(`/api/enrollments${query(values)}`),
  enroll: (classId, memberId) => post(`/api/classes/${classId}/enroll`, { ...(memberId ? { memberId: Number(memberId) } : {}), paymentMethod: 'Cash' }),
  cancelEnrollment: (id) => httpClient.patch(`/api/enrollments/${id}/cancel`, {}, { notifyForbidden: false }),
  sessions: (values) => httpClient.get(`/api/sessions${query(values)}`),
  rooms: () => httpClient.get('/api/rooms'),
  createSession: (data) => post('/api/sessions', data),
  roster: (classId) => httpClient.get(`/api/classes/${classId}/enrollments`),
  attendance: (values) => httpClient.get(`/api/attendances${query(values)}`),
  record: (data) => post('/api/training-results', data),
  results: (values) => httpClient.get(`/api/training-results${query(values)}`),
  feedbackMine: (classId) => httpClient.get(`/api/classes/${classId}/feedback/me`),
  feedback: (classId, data) => post(`/api/classes/${classId}/feedback`, { ...data, classId }),
  classFeedback: (classId) => httpClient.get(`/api/classes/${classId}/feedback`),
  coachFeedback: (classId) => httpClient.get(`/api/coaches/me/feedback${query({ classId })}`),
};
