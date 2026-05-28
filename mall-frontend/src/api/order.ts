import { http } from '@/api/http';
import type { Result } from '@/api/user';

export interface OrderCreateItemRequest {
  skuId: number;
  quantity: number;
}

export interface OrderCreateRequest {
  requestId: string;
  addressId: number;
  items: OrderCreateItemRequest[];
  remark?: string;
}

export interface OrderCreateResult {
  orderId: number;
  orderNo: string;
  totalAmount: number;
  payAmount: number;
  status: number;
}

export interface OrderItem {
  id: number;
  skuId: number;
  skuCode: string;
  productName: string;
  skuName: string;
  specData: string;
  mainImageUrl: string;
  unitPrice: number;
  quantity: number;
  totalAmount: number;
}

export interface OrderDetail {
  orderId: number;
  orderNo: string;
  userId: number;
  totalAmount: number;
  payAmount: number;
  freightAmount: number;
  status: number;
  cancelType: number | null;
  cancelReason: string | null;
  receiverName: string;
  receiverPhone: string;
  receiverProvince: string;
  receiverCity: string;
  receiverDistrict: string;
  receiverDetailAddress: string;
  remark: string | null;
  createTime: string;
  items: OrderItem[];
}

export interface OrderMain {
  orderId: number;
  orderNo: string;
  userId: number;
  totalAmount: number;
  payAmount: number;
  freightAmount: number;
  status: number;
  cancelType: number | null;
  cancelReason: string | null;
  createTime: string;
}

export interface PaymentPayResult {
  paymentNo: string;
  orderId: number;
  orderNo: string;
  orderStatus: number;
  paymentStatus: number;
  payAmount: number;
}

export type MockPaymentResult = 'SUCCESS' | 'FAILED';

export function createOrder(request: OrderCreateRequest) {
  return http.post<Result<OrderCreateResult>>('/api/orders', request);
}

export function getOrderDetail(orderId: number) {
  return http.get<Result<OrderDetail>>(`/api/orders/${orderId}`);
}

export function listOrders(userId: number) {
  return http.get<Result<OrderMain[]>>('/api/orders', { params: { userId } });
}

export function payOrder(orderId: number) {
  return http.post<Result<PaymentPayResult>>(`/api/orders/${orderId}/pay`);
}

export function callbackPayment(paymentNo: string, mockResult: MockPaymentResult) {
  return http.post<Result<PaymentPayResult>>(`/api/payments/${paymentNo}/callback`, { mockResult });
}
