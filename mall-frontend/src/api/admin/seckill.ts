import { http } from '@/api/http';
import type { PageResult, Result } from '@/api/product';

export interface AdminSeckillSku {
  id: number;
  activityId: number;
  skuId: number;
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
  id: number;
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
  skuId: number;
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

export function getAdminSeckillActivity(activityId: number) {
  return http.get<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}`);
}

export function createAdminSeckillActivity(request: AdminSeckillActivityRequest) {
  return http.post<Result<AdminSeckillActivity>>('/api/admin/seckill/activities', request);
}

export function updateAdminSeckillActivity(activityId: number, request: AdminSeckillActivityRequest) {
  return http.put<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}`, request);
}

export function deleteAdminSeckillActivity(activityId: number) {
  return http.delete<Result<void>>(`/api/admin/seckill/activities/${activityId}`);
}

export function updateAdminSeckillActivityStatus(activityId: number, status: number) {
  return http.put<Result<AdminSeckillActivity>>(`/api/admin/seckill/activities/${activityId}/status`, { status });
}

export function preheatAdminSeckillActivity(activityId: number) {
  return http.post<Result<void>>(`/api/admin/seckill/activities/${activityId}/preheat`);
}

export function addAdminSeckillSku(activityId: number, request: AdminSeckillSkuRequest) {
  return http.post<Result<AdminSeckillSku>>(`/api/admin/seckill/activities/${activityId}/skus`, request);
}

export function updateAdminSeckillSku(activityId: number, seckillSkuId: number, request: AdminSeckillSkuRequest) {
  return http.put<Result<AdminSeckillSku>>(`/api/admin/seckill/activities/${activityId}/skus/${seckillSkuId}`, request);
}

export function deleteAdminSeckillSku(activityId: number, seckillSkuId: number) {
  return http.delete<Result<void>>(`/api/admin/seckill/activities/${activityId}/skus/${seckillSkuId}`);
}
