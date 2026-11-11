import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Merchant } from '../../account/data-access/account.models';
import { Page, pageParams } from '../../../shared/models/page';
@Injectable({ providedIn: 'root' })
export class MerchantsApi {
  private readonly http = inject(HttpClient);
  list(page = 0, sort = 'shopName,asc') {
    return this.http.get<Page<Merchant>>('/api/v1/merchants', {
      params: pageParams(page, 12, sort),
    });
  }
  get(id: string) {
    return this.http.get<Merchant>(`/api/v1/merchants/${encodeURIComponent(id)}`);
  }
}
