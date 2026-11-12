import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Customer, Merchant, CustomerRegistration, MerchantRegistration } from './account.models';
@Injectable({ providedIn: 'root' })
export class AccountApi {
  private readonly http = inject(HttpClient);
  customerMe() {
    return this.http.get<Customer>('/api/v1/customers/me');
  }
  merchantMe() {
    return this.http.get<Merchant>('/api/v1/merchants/me');
  }
  registerCustomer(body: CustomerRegistration) {
    return this.http.post<Customer>('/api/v1/auth/customers/register', body);
  }
  registerMerchant(body: MerchantRegistration) {
    return this.http.post<Merchant>('/api/v1/auth/merchants/register', body);
  }
}
