import { http } from '@/api/http';
import type { PageResult, Result } from '@/api/product';

export interface AdminProductSku {
  id?: number;
  productId?: number;
  productName?: string;
  productCode?: string;
  skuCode: string;
  skuName: string;
  specData?: string;
  price: number;
  originalPrice?: number;
  mainImageUrl?: string;
  status: number;
  availableStock: number;
  lockedStock?: number;
}

export interface AdminProduct {
  id: number;
  categoryId: number;
  productCode: string;
  name: string;
  subtitle?: string;
  mainImageUrl?: string;
  description?: string;
  status: number;
  sort: number;
  skus?: AdminProductSku[];
}

export interface AdminProductRequest {
  categoryId: number | null;
  productCode: string;
  name: string;
  subtitle?: string;
  mainImageUrl?: string;
  description?: string;
  status: number;
  sort: number;
  skus: AdminProductSku[];
}

export function listAdminProducts(params?: {
  pageNo?: number;
  pageSize?: number;
  categoryId?: number;
  keyword?: string;
  status?: number;
}) {
  return http.get<Result<PageResult<AdminProduct>>>('/api/admin/products', { params });
}

export function getAdminProduct(productId: number) {
  return http.get<Result<AdminProduct>>(`/api/admin/products/${productId}`);
}

export function createAdminProduct(request: AdminProductRequest) {
  return http.post<Result<AdminProduct>>('/api/admin/products', request);
}

export function updateAdminProduct(productId: number, request: AdminProductRequest) {
  return http.put<Result<AdminProduct>>(`/api/admin/products/${productId}`, request);
}

export function deleteAdminProduct(productId: number) {
  return http.delete<Result<void>>(`/api/admin/products/${productId}`);
}

export function updateAdminProductStatus(productId: number, status: number) {
  return http.put<Result<AdminProduct>>(`/api/admin/products/${productId}/status`, { status });
}

export function listAdminSelectableSkus(params?: { pageNo?: number; pageSize?: number; keyword?: string }) {
  return http.get<Result<PageResult<AdminProductSku>>>('/api/admin/products/skus', { params });
}
