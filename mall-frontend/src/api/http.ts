import axios from 'axios';

export const http = axios.create({
  baseURL: '/',
  timeout: 8000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('mall-lite-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
