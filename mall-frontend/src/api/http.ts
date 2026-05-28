import axios from 'axios';

import { getStoredToken } from '@/utils/authStorage';

export const http = axios.create({
  baseURL: '/',
  timeout: 8000
});

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
