import { http } from '@/api/http';
import type { PageResult, Result } from '@/api/product';

export interface AdminSeckillSku {
  id: string;
  activityId: string;
  skuId: string;
  productName?: string;
  skuName?: string;
  originalPrice?: number;
  seckillPrice: number;
  stockCount: number;
  limitQuantity: number;
  sort: number;
  status: number;
}

export interface AdminSeckillActivity {
  id: string;
  name: string;
  startTime: string;
  endTime: string;
  status: number;
  remark?: string;
  skus: AdminSeckillSku[];
}

export interface AdminSeckillActivityRequest {
  name: string;
  startTime: string;
  endTime: string;
  status: number;
  remark?: string;
}

export interface AdminSeckillSkuRequest {
  skuId: number | string;
  seckillPrice: number;
  stockCount: number;
  limitQuantity: number;
  sort: number;
  status: number;
}

export function listAdminSeckillActivities(params?: {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}) {
  return http.get<Result<PageResult<AdminSeckillActivity>>>('/api/admin/seckill/activities', { params });
}

export function getAdminSeckillActivity(activityId: string) {
  return http.get<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}`);
}

export function createAdminSeckillActivity(request: AdminSeckillActivityRequest) {
  return http.post<Result<AdminSeckillActivity>>('/api/admin/seckill/activities', request);
}

export function updateAdminSeckillActivity(activityId: string, request: AdminSeckillActivityRequest) {
  return http.put<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}`, request);
}

export function deleteAdminSeckillActivity(activityId: string) {
  return http.delete<Result<void>>(`/api/admin/seckill/activities/${activityId}`);
}

export function updateAdminSeckillActivityStatus(activityId: string, status: number) {
  return http.put<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}/status`, { status });
}

export function preheatAdminSeckillActivity(activityId: string) {
  return http.post<Result<void>>(`/api/admin/seckill/activities/${activityId}/preheat`);
}

export function addAdminSeckillSku(activityId: string, request: AdminSeckillSkuRequest) {
  return http.post<Result<AdminSeckillSku>>(`/api/admin/seckill/activities/${activityId}/skus`, request);
}

export function updateAdminSeckillSku(activityId: string, seckillSkuId: string, request: AdminSeckillSkuRequest) {
  return http.put<Result<AdminSeckillSku>>(`/api/admin/seckill/activities/${activityId}/skus/${seckillSkuId}`, request);
}

export function deleteAdminSeckillSku(activityId: string, seckillSkuId: string) {
  return http.delete<Result<void>>(`/api/admin/seckill/activities/${activityId}/skus/${seckillSkuId}`);
}
