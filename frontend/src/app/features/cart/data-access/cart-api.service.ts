import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, of, throwError } from 'rxjs';
import { apiFailure } from '../../../core/http/api-error';
export interface CartItem {
  readonly id: string;
  readonly cartId: string;
  readonly productId: string;
  readonly productName: string;
  readonly priceCents: number | null;
  readonly quantity: number;
  readonly totalPriceCents: number | null;
  readonly productActive: boolean;
  readonly stockQuantity: number;
}
export interface Cart {
  readonly id: string;
  readonly customerId: string;
  readonly totalPriceCents: number;
  readonly cartItems: readonly CartItem[];
}
@Injectable({ providedIn: 'root' })
export class CartApi {
  private readonly http = inject(HttpClient);
  get() {
    return this.http.get<Cart>('/api/v1/carts').pipe(
      catchError((error: unknown) => {
        const failure = apiFailure(error);
        return failure.missingCart ? of(null) : throwError(() => failure);
      })
    );
  }
  add(productId: string, quantity: number) {
    return this.http.post<CartItem>('/api/v1/carts/items', { productId, quantity });
  }
  remove(id: string) {
    return this.http.delete<void>(`/api/v1/carts/items/${encodeURIComponent(id)}`);
  }
  clear() {
    return this.http.delete<void>('/api/v1/carts/items');
  }
}
