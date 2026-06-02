import { http } from '@/api/http';
import type { Result } from '@/api/user';

export interface CartItem {
  skuId: number;
  productId: number;
  productName: string;
  skuName: string;
  mainImageUrl: string;
  price: number;
  quantity: number;
  available: boolean;
}

export function addCartItem(skuId: number, quantity: number) {
  return http.post<Result<void>>('/api/cart/items', { skuId, quantity });
}

export function listCartItems() {
  return http.get<Result<CartItem[]>>('/api/cart/items');
}

export function updateCartItemQuantity(skuId: number, quantity: number) {
  return http.put<Result<void>>(`/api/cart/items/${skuId}`, { quantity });
}

export function deleteCartItem(skuId: number) {
  return http.delete<Result<void>>(`/api/cart/items/${skuId}`);
}

export function deleteCartItems(skuIds: number[]) {
  return http.delete<Result<void>>('/api/cart/items', { data: { skuIds } });
}
