import { http } from '@/api/http';
import type { OrderCreateResult } from '@/api/order';
import type { Result } from '@/api/user';

export interface SeckillSku {
  id: number;
  activityId: number;
  skuId: number;
  productName: string;
  skuName: string;
  mainImageUrl: string;
  seckillPrice: number;
  stockCount: number;
  limitQuantity: number;
  status: number;
}

export interface SeckillActivity {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  status: number;
  skus: SeckillSku[];
}

export interface SeckillOrderCreateRequest {
  seckillSkuId: number;
  requestId: string;
  addressId: number;
  quantity: number;
  remark?: string;
}

export function listActiveSeckillActivities() {
  return http.get<Result<SeckillActivity[]>>('/api/seckill/activities/active');
}

export function createSeckillOrder(request: SeckillOrderCreateRequest) {
  return http.post<Result<OrderCreateResult>>('/api/seckill/orders', request);
}
