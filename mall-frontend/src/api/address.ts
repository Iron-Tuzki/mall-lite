import { http } from '@/api/http';
import type { Result } from '@/api/user';

export interface AddressItem {
  id: number;
  userId: number;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  postalCode: string | null;
  defaultFlag: number;
}

export interface AddressRequest {
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  postalCode?: string | null;
  defaultFlag: number;
}

export function listAddresses(userId: number) {
  return http.get<Result<AddressItem[]>>(`/api/users/${userId}/addresses`);
}

export function createAddress(userId: number, request: AddressRequest) {
  return http.post<Result<AddressItem>>(`/api/users/${userId}/addresses`, request);
}

export function updateAddress(userId: number, addressId: number, request: AddressRequest) {
  return http.put<Result<AddressItem>>(`/api/users/${userId}/addresses/${addressId}`, request);
}

export function deleteAddress(userId: number, addressId: number) {
  return http.delete<Result<void>>(`/api/users/${userId}/addresses/${addressId}`);
}
