import { http } from '@/api/http';

export interface CategoryItem {
  id: number;
  name: string;
}

export interface ProductSummary {
  id: number;
  categoryId: number;
  productCode: string;
  name: string;
  subtitle: string;
  mainImageUrl: string;
  minPrice: number | null;
}

export interface SkuItem {
  id: number;
  productId: number;
  skuCode: string;
  skuName: string;
  specData: string;
  price: number;
  originalPrice: number;
  mainImageUrl: string;
}

export interface ProductDetail extends ProductSummary {
  description: string;
  skus: SkuItem[];
}

export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface CursorPageResult<T> {
  records: T[];
  nextSort: number | null;
  nextId: number | null;
  hasMore: boolean;
}

export function listCategories() {
  return http.get<Result<CategoryItem[]>>('/api/categories');
}

export function listProducts(params?: { categoryId?: number; keyword?: string }) {
  return http.get<Result<ProductSummary[]>>('/api/products', { params });
}

export function listRecommendProducts(params?: { pageNo?: number; pageSize?: number }) {
  return http.get<Result<PageResult<ProductSummary>>>('/api/products/recommend', { params });
}

export function scrollRecommendProducts(params?: { pageSize?: number; lastSort?: number | null; lastId?: number | null }) {
  return http.get<Result<CursorPageResult<ProductSummary>>>('/api/products/recommend/scroll', { params });
}

export function getProductDetail(productId: number) {
  return http.get<Result<ProductDetail>>(`/api/products/${productId}`);
}
