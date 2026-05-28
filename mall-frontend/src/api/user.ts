import { http } from '@/api/http';

export interface Result<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
}

export interface UserProfile {
  id: number;
  username: string;
  nickname: string | null;
  phone: string | null;
  email: string | null;
  avatarUrl: string | null;
  status: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  user: UserProfile;
}

export interface SignInProfile {
  todaySigned: boolean;
  monthSignedCount: number;
  continuousSignedDays: number;
  year: number;
  month: number;
  daysInMonth: number;
  signedDays: number[];
}

export function login(request: LoginRequest) {
  return http.post<Result<LoginResult>>('/api/users/login', request);
}

export function getCurrentUser() {
  return http.get<Result<UserProfile>>('/api/users/me');
}

export function logout() {
  return http.post<Result<void>>('/api/users/logout');
}

export function getSignInProfile() {
  return http.get<Result<SignInProfile>>('/api/users/sign-in/profile');
}

export function signInToday() {
  return http.post<Result<SignInProfile>>('/api/users/sign-in');
}
