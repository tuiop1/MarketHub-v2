import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Page, pageParams } from '../../../shared/models/page';
export type PaymentMethod = 'CARD' | 'GOOGLE_PAY' | 'QR';
export type OrderStatus =
  | 'PENDING_RESERVATION'
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PAYMENT_FAILED'
  | 'CANCELLED'
  | 'COMPENSATION_FAILED';
export interface PurchaseItem {
  readonly productId: string;
  readonly quantity: number;
}
export interface OrderItem {
  readonly id: string;
  readonly productId: string;
  readonly merchantId: string;
  readonly productNameSnapshot: string;
  readonly merchantNameSnapshot: string;
  readonly priceSnapshotCents: number;
  readonly quantity: number;
  readonly totalPriceSnapshotCents: number;
}
export interface Order {
  readonly id: string;
  readonly status: OrderStatus;
  readonly totalPriceCents: number;
  readonly items: readonly OrderItem[];
  readonly createdAt: string;
}
@Injectable({ providedIn: 'root' })
export class OrdersApi {
  private readonly http = inject(HttpClient);
  list(page = 0, sort = 'createdAt,desc') {
    return this.http.get<Page<Order>>('/api/v1/orders/me', { params: pageParams(page, 10, sort) });
  }
  purchase(items: readonly PurchaseItem[], paymentMethod: PaymentMethod) {
    return this.http.post<Order>('/api/v1/orders/purchase', { items, paymentMethod });
  }
  purchaseCart(paymentMethod: PaymentMethod) {
    return this.http.post<Order>('/api/v1/orders/my-cart/purchase', JSON.stringify(paymentMethod), {
      headers: { 'Content-Type': 'application/json' },
    });
  }
}
