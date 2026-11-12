import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Page, pageParams } from '../../../shared/models/page';
import { Product, ProductCreate, ProductUpdate } from '../../catalog/data-access/catalog.models';
@Injectable({ providedIn: 'root' })
export class MerchantProductsApi {
  private readonly http = inject(HttpClient);
  list(page = 0, sort = 'createdAt,desc') {
    return this.http.get<Page<Product>>('/api/v1/merchant/products', {
      params: pageParams(page, 10, sort),
    });
  }
  get(id: string) {
    return this.http.get<Product>(`/api/v1/merchant/products/${encodeURIComponent(id)}`);
  }
  create(body: ProductCreate) {
    return this.http.post<Product>('/api/v1/merchant/products', body);
  }
  update(id: string, body: ProductUpdate) {
    return this.http.put<Product>(`/api/v1/merchant/products/${encodeURIComponent(id)}`, body);
  }
  deactivate(id: string) {
    return this.http.delete<void>(`/api/v1/merchant/products/${encodeURIComponent(id)}`);
  }
}
