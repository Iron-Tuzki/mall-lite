const CART_CHECKOUT_KEY = 'mall-lite-cart-checkout';

export interface CheckoutItem {
  productId: number;
  skuId: number;
  quantity: number;
}

export function setCartCheckoutItems(items: CheckoutItem[]) {
  sessionStorage.setItem(CART_CHECKOUT_KEY, JSON.stringify(items));
}

export function getCartCheckoutItems(): CheckoutItem[] {
  const rawValue = sessionStorage.getItem(CART_CHECKOUT_KEY);
  if (!rawValue) {
    return [];
  }
  try {
    const items = JSON.parse(rawValue) as CheckoutItem[];
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

export function clearCartCheckoutItems() {
  sessionStorage.removeItem(CART_CHECKOUT_KEY);
}
