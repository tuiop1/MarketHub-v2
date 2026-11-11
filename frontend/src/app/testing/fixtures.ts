import { signal } from '@angular/core';
import { Page } from '../shared/models/page';
import { Product, Category } from '../features/catalog/data-access/catalog.models';
import { Merchant, Customer } from '../features/account/data-access/account.models';
import { Cart, CartItem } from '../features/cart/data-access/cart-api.service';
import { Order } from '../features/orders/data-access/orders-api.service';
export const product: Product = {
  id: 'bc7c9dcf-7114-4b70-902c-158ce189e609',
  merchantId: 'merchant-1',
  categoryId: 'category-1',
  name: 'Field notebook',
  description: 'A notebook for everyday ideas.',
  priceCents: 1299,
  stockQuantity: 4,
  active: true,
  createdAt: '2026-11-01T10:00:00Z',
  updatedAt: '2026-11-01T10:00:00Z',
};
export const category: Category = {
  id: 'category-1',
  name: 'Stationery',
  description: null,
  active: true,
  createdAt: product.createdAt,
};
export const merchant: Merchant = {
  id: 'merchant-1',
  shopName: 'Paper shop',
  description: null,
  email: 'shop@example.test',
  status: 'PENDING',
  createdAt: product.createdAt,
  updatedAt: product.updatedAt,
  verifiedAt: null,
};
export const customer: Customer = {
  id: 'customer-1',
  firstName: 'Alex',
  lastName: 'Test',
  birthDate: null,
  email: 'alex@example.test',
  address: null,
  enabled: true,
  createdAt: product.createdAt,
  updatedAt: product.updatedAt,
};
export const cartItem: CartItem = {
  id: 'cart-item-1',
  cartId: 'cart-1',
  productId: product.id,
  productName: product.name,
  priceCents: 1299,
  quantity: 1,
  totalPriceCents: 1299,
  productActive: true,
  stockQuantity: 4,
};
export const cart: Cart = {
  id: 'cart-1',
  customerId: customer.id,
  totalPriceCents: 1299,
  cartItems: [cartItem],
};
export const order: Order = {
  id: 'order-1',
  status: 'PAID',
  totalPriceCents: 1299,
  items: [
    {
      id: 'order-item-1',
      productId: product.id,
      merchantId: merchant.id,
      productNameSnapshot: product.name,
      merchantNameSnapshot: merchant.shopName,
      priceSnapshotCents: 1299,
      quantity: 1,
      totalPriceSnapshotCents: 1299,
    },
  ],
  createdAt: product.createdAt,
};
export function page<T>(content: readonly T[], number = 0, totalPages = 1): Page<T> {
  return {
    content,
    totalElements: content.length,
    totalPages,
    number,
    size: 12,
    first: number === 0,
    last: number >= totalPages - 1,
  };
}
export function authStub(roles: readonly string[] = []) {
  const roleState = signal(roles);
  const authenticated = signal(roles.length > 0);
  return {
    authenticated,
    roles: roleState,
    displayName: signal('Alex'),
    error: signal(''),
    sessionEnded: signal(false),
    isMerchant: () => roleState().some((role) => role.startsWith('MERCHANT')),
    hasRole: (role: string) => authenticated() && roleState().includes(role),
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    token: vi.fn().mockResolvedValue('access-token'),
    endSession: vi.fn(() => authenticated.set(false)),
  };
}
