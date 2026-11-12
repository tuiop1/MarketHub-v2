import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Page, pageParams } from '../../../shared/models/page';
import { Category, Product } from './catalog.models';
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);
  products(page = 0, sort = 'name,asc') {
    return this.http.get<Page<Product>>('/api/v1/products', { params: pageParams(page, 12, sort) });
  }
  product(id: string) {
    return this.http.get<Product>(`/api/v1/products/${encodeURIComponent(id)}`);
  }
  categories(page = 0) {
    return this.http.get<Page<Category>>('/api/v1/categories', {
      params: pageParams(page, 24, 'name,asc'),
    });
  }
  category(id: string) {
    return this.http.get<Category>(`/api/v1/categories/${encodeURIComponent(id)}`);
  }
}
