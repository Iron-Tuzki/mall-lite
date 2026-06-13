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

export interface RegisterRequest {
  username: string;
  password: string;
  nickname?: string;
  phone?: string;
  email?: string;
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

export interface SignInMonthProfile {
  month: number;
  daysInMonth: number;
  signedCount: number;
  signedDays: number[];
}

export interface SignInYearlyProfile {
  year: number;
  yearSignedCount: number;
  months: SignInMonthProfile[];
}

export function login(request: LoginRequest) {
  return http.post<Result<LoginResult>>('/api/users/login', request);
}

export function register(request: RegisterRequest) {
  return http.post<Result<UserProfile>>('/api/users/register', request);
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

export function getSignInYearlyProfile(year?: number) {
  return http.get<Result<SignInYearlyProfile>>('/api/users/sign-in/yearly', {
    params: year ? { year } : undefined
  });
}

export function signInToday() {
  return http.post<Result<SignInProfile>>('/api/users/sign-in');
}
