import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Page, pageParams } from '../../../shared/models/page';
import { Category } from '../../catalog/data-access/catalog.models';
import { Customer, Merchant } from '../../account/data-access/account.models';
export interface CategoryWrite {
  readonly name: string;
  readonly description: string;
}
export type MerchantAction = 'verify' | 'reject' | 'disable';
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  categories(page = 0) {
    return this.http.get<Page<Category>>('/api/v1/admin/categories', {
      params: pageParams(page, 10, 'name,asc'),
    });
  }
  createCategory(body: CategoryWrite) {
    return this.http.post<Category>('/api/v1/admin/categories', body);
  }
  updateCategory(id: string, body: CategoryWrite) {
    return this.http.put<Category>(`/api/v1/admin/categories/${encodeURIComponent(id)}`, body);
  }
  setCategoryEnabled(id: string, enabled: boolean) {
    return this.http.patch<Category>(
      `/api/v1/admin/categories/${encodeURIComponent(id)}/${enabled ? 'enable' : 'disable'}`,
      {}
    );
  }
  pendingMerchants(page = 0) {
    return this.http.get<Page<Merchant>>('/api/v1/admin/merchants/unverified', {
      params: pageParams(page, 10, 'createdAt,asc'),
    });
  }
  merchantAction(id: string, action: MerchantAction) {
    return this.http.patch<void>(`/api/v1/admin/merchants/${encodeURIComponent(id)}/${action}`, {});
  }
  customers(page = 0) {
    return this.http.get<Page<Customer>>('/api/v1/admin/customers', {
      params: pageParams(page, 10, 'createdAt,desc'),
    });
  }
  setCustomerEnabled(id: string, enabled: boolean) {
    return this.http.patch<void>(
      `/api/v1/admin/customers/${encodeURIComponent(id)}/${enabled ? 'enable' : 'disable'}`,
      {}
    );
  }
}
