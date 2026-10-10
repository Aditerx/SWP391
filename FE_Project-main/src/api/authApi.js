import { httpClient } from './httpClient.js';

export const login = (email, password) => httpClient.post(
  '/api/auth/login',
  { email, password },
  { notifyUnauthorized: false },
);

export const getCurrentUser = () => httpClient.get('/api/auth/me', { notifyUnauthorized: false });
export const logout = () => httpClient.post('/api/auth/logout');
export const register = (data) => httpClient.post('/api/auth/register', data);
export const verifyRegistration = (email, otp) => httpClient.post('/api/auth/verify-registration', { email, otp });
export const resendOtp = (email) => httpClient.post('/api/auth/resend-otp', { email, purpose: 'REGISTER' });
