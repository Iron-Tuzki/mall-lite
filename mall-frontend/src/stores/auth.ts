import { defineStore } from 'pinia';

import { getCurrentUser, login, logout, type UserProfile } from '@/api/user';
import { getStoredToken, removeStoredToken, setStoredToken } from '@/utils/authStorage';

interface AuthState {
  token: string | null;
  user: UserProfile | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getStoredToken(),
    user: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录',
    maskedPhone: (state) => maskPhone(state.user?.phone)
  },
  actions: {
    async login(username: string, password: string) {
      const response = await login({ username, password });
      if (!response.data.success) {
        throw new Error(response.data.message || '登录失败');
      }
      this.token = response.data.data.token;
      this.user = response.data.data.user;
      setStoredToken(this.token);
    },
    async fetchCurrentUser() {
      if (!this.token) {
        return null;
      }
      const response = await getCurrentUser();
      if (!response.data.success) {
        this.clearAuth();
        throw new Error(response.data.message || '登录状态已失效');
      }
      this.user = response.data.data;
      return this.user;
    },
    async logout() {
      if (this.token) {
        await logout().catch(() => undefined);
      }
      this.clearAuth();
    },
    clearAuth() {
      this.token = null;
      this.user = null;
      removeStoredToken();
    }
  }
});

function maskPhone(phone?: string | null) {
  if (!phone) {
    return '未绑定手机号';
  }
  if (phone.length < 7) {
    return phone;
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}
