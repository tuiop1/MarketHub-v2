import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends cart payment methods as the enum JSON value expected by the backend', () => {
    api.purchaseCart('CARD').subscribe();

    const request = http.expectOne('/api/v1/orders/my-cart/purchase');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBe('"CARD"');
    expect(request.request.headers.get('Content-Type')).toBe('application/json');
    request.flush({});
  });

  it('clamps negative page indexes to zero', () => {
    api.products(-1).subscribe();

    const request = http.expectOne(value => value.url === '/api/v1/products');
    expect(request.request.params.get('page')).toBe('0');
    request.flush({ content: [] });
  });
});
