import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface Category {
  id: string;
  name: string;
  description: string;
  active: boolean;
  createdAt?: string;
}

export interface Product {
  id: string;
  merchantId: string;
  categoryId: string;
  name: string;
  description: string;
  priceCents: number;
  stockQuantity: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Merchant {
  id: string;
  shopName: string;
  description: string;
  email: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  verifiedAt?: string | null;
}

export interface Address {
  country: string;
  city: string;
  street: string;
  postalCode: string;
  apartment?: string;
}

export interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  email: string;
  address: Address;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  priceCents: number;
  quantity: number;
  totalPriceCents: number;
  productActive: boolean;
  stockQuantity: number;
}

export interface Cart {
  id: string;
  customerId: string;
  totalPriceCents: number;
  cartItems: CartItem[];
}

export interface OrderItem {
  id: string;
  productId: string;
  merchantId: string;
  productNameSnapshot: string;
  merchantNameSnapshot: string;
  priceSnapshotCents: number;
  quantity: number;
  totalPriceSnapshotCents: number;
}

export interface Order {
  id: string;
  status: string;
  totalPriceCents: number;
  items: OrderItem[];
  createdAt: string;
}

export interface PurchaseItem {
  productId: string;
  quantity: number;
}

export interface ProductInput {
  name: string;
  description: string;
  priceCents: number;
  stockQuantity: number;
  categoryId: string;
  active?: boolean;
}

export type PaymentMethod = 'CARD' | 'GOOGLE_PAY' | 'QR';
export type MerchantAction = 'verify' | 'reject' | 'disable';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  categories(page = 0, size = 12, sort = 'name,asc') {
    return this.http.get<Page<Category>>('/api/v1/categories', {
      params: this.pageParams(page, size, sort)
    });
  }

  category(id: string) {
    return this.http.get<Category>(`/api/v1/categories/${id}`);
  }

  adminCategories(page = 0, size = 10, sort = 'name,asc') {
    return this.http.get<Page<Category>>('/api/v1/admin/categories', {
      params: this.pageParams(page, size, sort)
    });
  }

  createCategory(body: { name: string; description: string }) {
    return this.http.post<Category>('/api/v1/admin/categories', body);
  }

  updateCategory(category: Pick<Category, 'id' | 'name' | 'description'>) {
    return this.http.put<Category>(`/api/v1/admin/categories/${category.id}`, {
      name: category.name,
      description: category.description
    });
  }

  setCategoryEnabled(id: string, enabled: boolean) {
    return this.http.patch<Category>(
      `/api/v1/admin/categories/${id}/${enabled ? 'enable' : 'disable'}`,
      {}
    );
  }

  products(page = 0, size = 12, sort = 'name,asc') {
    return this.http.get<Page<Product>>('/api/v1/products', {
      params: this.pageParams(page, size, sort)
    });
  }

  product(id: string) {
    return this.http.get<Product>(`/api/v1/products/${id}`);
  }

  merchantProducts(page = 0, size = 10, sort = 'createdAt,desc') {
    return this.http.get<Page<Product>>('/api/v1/merchant/products', {
      params: this.pageParams(page, size, sort)
    });
  }

  merchantProduct(id: string) {
    return this.http.get<Product>(`/api/v1/merchant/products/${id}`);
  }

  createProduct(body: ProductInput) {
    const { active: _active, ...request } = body;
    return this.http.post<Product>('/api/v1/merchant/products', request);
  }

  updateProduct(product: ProductInput & { id: string; active: boolean }) {
    return this.http.put<Product>(`/api/v1/merchant/products/${product.id}`, {
      name: product.name,
      description: product.description,
      priceCents: product.priceCents,
      stockQuantity: product.stockQuantity,
      categoryId: product.categoryId,
      active: product.active
    });
  }

  deleteProduct(id: string) {
    return this.http.delete<void>(`/api/v1/merchant/products/${id}`);
  }

  merchants(page = 0, size = 12, sort = 'shopName,asc') {
    return this.http.get<Page<Merchant>>('/api/v1/merchants', {
      params: this.pageParams(page, size, sort)
    });
  }

  merchant(id: string) {
    return this.http.get<Merchant>(`/api/v1/merchants/${id}`);
  }

  unverifiedMerchants(page = 0, size = 10, sort = 'createdAt,asc') {
    return this.http.get<Page<Merchant>>('/api/v1/admin/merchants/unverified', {
      params: this.pageParams(page, size, sort)
    });
  }

  merchantAction(id: string, action: MerchantAction) {
    return this.http.patch<void>(`/api/v1/admin/merchants/${id}/${action}`, {});
  }

  registerCustomer(body: unknown) {
    return this.http.post<Customer>('/api/v1/auth/customers/register', body);
  }

  registerMerchant(body: unknown) {
    return this.http.post<Merchant>('/api/v1/auth/merchants/register', body);
  }

  customerMe() {
    return this.http.get<Customer>('/api/v1/customers/me');
  }

  merchantMe() {
    return this.http.get<Merchant>('/api/v1/merchants/me');
  }

  cart() {
    return this.http.get<Cart>('/api/v1/carts');
  }

  addToCart(productId: string, quantity: number) {
    return this.http.post<CartItem>('/api/v1/carts/items', { productId, quantity });
  }

  removeCartItem(id: string) {
    return this.http.delete<void>(`/api/v1/carts/items/${id}`);
  }

  clearCart() {
    return this.http.delete<void>('/api/v1/carts/items');
  }

  purchaseCart(paymentMethod: PaymentMethod) {
    return this.http.post<Order>(
      '/api/v1/orders/my-cart/purchase',
      JSON.stringify(paymentMethod),
      { headers: { 'Content-Type': 'application/json' } }
    );
  }

  purchase(items: PurchaseItem[], paymentMethod: PaymentMethod) {
    return this.http.post<Order>('/api/v1/orders/purchase', { items, paymentMethod });
  }

  orders(page = 0, size = 10, sort = 'createdAt,desc') {
    return this.http.get<Page<Order>>('/api/v1/orders/me', {
      params: this.pageParams(page, size, sort)
    });
  }

  private pageParams(page: number, size: number, sort: string): HttpParams {
    return new HttpParams()
      .set('page', Math.max(0, page))
      .set('size', size)
      .set('sort', sort);
  }
}
