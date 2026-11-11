import { Observable } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogApi } from '../features/catalog/data-access/catalog-api.service';
import { AccountApi } from '../features/account/data-access/account-api.service';
import { MerchantsApi } from '../features/merchants/data-access/merchants-api.service';
import { CartApi } from '../features/cart/data-access/cart-api.service';
import { OrdersApi } from '../features/orders/data-access/orders-api.service';
import { MerchantProductsApi } from '../features/merchant-dashboard/data-access/merchant-products-api.service';
import { AdminApi } from '../features/administration/data-access/admin-api.service';
import { apiErrorInterceptor } from '../core/http/api-error.interceptor';
import { product, cartItem, order, page, merchant, category, customer } from './fixtures';
describe('Gateway contracts', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('sends zero-based product pagination and supported sort', () => {
    TestBed.inject(CatalogApi).products(2, 'priceCents,desc').subscribe();
    const request = http.expectOne((req) => req.url === '/api/v1/products');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('12');
    expect(request.request.params.get('sort')).toBe('priceCents,desc');
    request.flush(page([product]));
  });
  it('purchases a cart using a JSON string enum, never an object', () => {
    TestBed.inject(OrdersApi).purchaseCart('QR').subscribe();
    const r = http.expectOne('/api/v1/orders/my-cart/purchase');
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toBe('"QR"');
    expect(r.request.headers.get('Content-Type')).toBe('application/json');
    r.flush({ ...order, status: 'PAYMENT_FAILED' });
  });
  it('posts direct purchase lines and the payment enum', () => {
    const items = [{ productId: product.id, quantity: 2 }];
    TestBed.inject(OrdersApi).purchase(items, 'GOOGLE_PAY').subscribe();
    const r = http.expectOne('/api/v1/orders/purchase');
    expect(r.request.body).toEqual({ items, paymentMethod: 'GOOGLE_PAY' });
    expect(r.request.method).toBe('POST');
    r.flush(order);
  });
  it('adds quantity and removes by cart item ID', () => {
    const api = TestBed.inject(CartApi);
    api.add(product.id, 2).subscribe();
    const add = http.expectOne('/api/v1/carts/items');
    expect(add.request.body).toEqual({ productId: product.id, quantity: 2 });
    add.flush(cartItem);
    api.remove(cartItem.id).subscribe();
    const remove = http.expectOne('/api/v1/carts/items/cart-item-1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
  it('treats only an explicit missing cart as empty', () => {
    let result: unknown = 'not set';
    TestBed.inject(CartApi)
      .get()
      .subscribe((value) => (result = value));
    http
      .expectOne('/api/v1/carts')
      .flush(
        { code: 'RESOURCE_NOT_FOUND', message: 'Cart was not found with customerId: customer-1' },
        { status: 404, statusText: 'Not Found' }
      );
    expect(result).toBeNull();
  });
  it('does not hide a cart enrichment failure', () => {
    let failure: unknown;
    TestBed.inject(CartApi)
      .get()
      .subscribe({ error: (error: unknown) => (failure = error) });
    http
      .expectOne('/api/v1/carts')
      .flush(
        { code: 'RESOURCE_NOT_FOUND', message: 'ProductResponse was not found with ids: x' },
        { status: 404, statusText: 'Not Found' }
      );
    expect(failure).toBeDefined();
  });
  it('separates merchant create and update DTOs', () => {
    const api = TestBed.inject(MerchantProductsApi);
    const body = {
      name: 'Notebook',
      description: 'Paper',
      priceCents: 100,
      stockQuantity: 1,
      categoryId: category.id,
    };
    api.create(body).subscribe();
    const create = http.expectOne('/api/v1/merchant/products');
    expect(create.request.body).toEqual(body);
    expect(create.request.body).not.toHaveProperty('active');
    create.flush(product);
    api.update(product.id, { ...body, active: false }).subscribe();
    const update = http.expectOne('/api/v1/merchant/products/' + product.id);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.active).toBe(false);
    update.flush(product);
    api.deactivate(product.id).subscribe();
    const remove = http.expectOne('/api/v1/merchant/products/' + product.id);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
  it('registers exact customer and merchant DTOs without login or demo values', () => {
    const api = TestBed.inject(AccountApi);
    const identity = {
      firstName: 'Alex',
      lastName: 'Test',
      email: 'alex@example.test',
      password: 'test-password',
    };
    api.registerCustomer({ ...identity, birthDate: null, address: null }).subscribe();
    const c = http.expectOne('/api/v1/auth/customers/register');
    expect(c.request.method).toBe('POST');
    expect(c.request.body).toEqual({ ...identity, birthDate: null, address: null });
    c.flush(customer);
    api.registerMerchant({ ...identity, shopName: 'Paper shop', description: '' }).subscribe();
    const m = http.expectOne('/api/v1/auth/merchants/register');
    expect(m.request.body.shopName).toBe('Paper shop');
    m.flush(merchant);
  });
  it('loads profiles and all public detail endpoints through the gateway', () => {
    TestBed.inject(AccountApi).customerMe().subscribe();
    http.expectOne('/api/v1/customers/me').flush(customer);
    TestBed.inject(AccountApi).merchantMe().subscribe();
    http.expectOne('/api/v1/merchants/me').flush(merchant);
    TestBed.inject(CatalogApi).product(product.id).subscribe();
    http.expectOne('/api/v1/products/' + product.id).flush(product);
    TestBed.inject(CatalogApi).category(category.id).subscribe();
    http.expectOne('/api/v1/categories/' + category.id).flush(category);
    TestBed.inject(MerchantsApi).get(merchant.id).subscribe();
    http.expectOne('/api/v1/merchants/' + merchant.id).flush(merchant);
  });
  it.each(['verify', 'reject', 'disable'] as const)(
    'uses the supported %s merchant transition',
    (action) => {
      TestBed.inject(AdminApi).merchantAction(merchant.id, action).subscribe();
      const r = http.expectOne('/api/v1/admin/merchants/' + merchant.id + '/' + action);
      expect(r.request.method).toBe('PATCH');
      expect(r.request.body).toEqual({});
      r.flush(null);
    }
  );
  it('uses category updates and activation endpoints', () => {
    const api = TestBed.inject(AdminApi);
    const body = { name: 'Stationery', description: '' };
    api.createCategory(body).subscribe();
    const c = http.expectOne('/api/v1/admin/categories');
    expect(c.request.method).toBe('POST');
    c.flush(category);
    api.updateCategory(category.id, body).subscribe();
    const u = http.expectOne('/api/v1/admin/categories/category-1');
    expect(u.request.method).toBe('PUT');
    expect(u.request.body).toEqual(body);
    u.flush(category);
    api.setCategoryEnabled(category.id, false).subscribe();
    const d = http.expectOne('/api/v1/admin/categories/category-1/disable');
    expect(d.request.method).toBe('PATCH');
    d.flush(category);
  });
  it.each([true, false])('sets customer enabled=%s', (enabled) => {
    TestBed.inject(AdminApi).setCustomerEnabled(customer.id, enabled).subscribe();
    const r = http.expectOne(
      '/api/v1/admin/customers/customer-1/' + (enabled ? 'enable' : 'disable')
    );
    expect(r.request.method).toBe('PATCH');
    r.flush(null);
  });
  it('lists categories, merchants, orders and administrative pages with pagination', () => {
    const calls: readonly (() => Observable<unknown>)[] = [
      () => TestBed.inject(CatalogApi).categories(1),
      () => TestBed.inject(MerchantsApi).list(1),
      () => TestBed.inject(OrdersApi).list(1),
      () => TestBed.inject(AdminApi).categories(1),
      () => TestBed.inject(AdminApi).pendingMerchants(1),
      () => TestBed.inject(AdminApi).customers(1),
      () => TestBed.inject(MerchantProductsApi).list(1),
    ];
    for (const call of calls) call().subscribe();
    for (const r of http.match(() => true)) {
      expect(r.request.url.startsWith('/api/v1/')).toBe(true);
      expect(r.request.params.get('page')).toBe('1');
      expect(r.request.method).toBe('GET');
      r.flush(page([]));
    }
  });
});
