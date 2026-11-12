import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminMerchantsPage } from './merchants.page';
import { AdminCustomersPage } from './customers.page';
import { CategoryFormComponent } from './category-form.component';
import { ProductFormComponent } from '../merchant-dashboard/product-form.component';
import { merchant, customer, category, page, product } from '../../testing/fixtures';
describe('Management workflows', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    // jsdom has no dialog implementation; browser focus behavior uses native <dialog>.
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = true;
      },
    });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = false;
      },
    });
  });
  afterEach(() => http.verify());
  it('requires confirmation before approving a merchant and supports cancellation', () => {
    const fixture = TestBed.createComponent(AdminMerchantsPage);
    http.expectOne((r) => r.url === '/api/v1/admin/merchants/unverified').flush(page([merchant]));
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button')
    );
    buttons.find((b) => b.textContent?.trim() === 'Approve')?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog[open]')).toBeTruthy();
    http.expectNone((r) => r.method === 'PATCH');
    fixture.nativeElement.querySelector('dialog button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog')).toBeNull();
    http.expectNone((r) => r.method === 'PATCH');
  });
  it('patches and reloads only after approval confirmation', async () => {
    const fixture = TestBed.createComponent(AdminMerchantsPage);
    http.expectOne((r) => r.url === '/api/v1/admin/merchants/unverified').flush(page([merchant]));
    fixture.detectChanges();
    fixture.componentInstance.confirm(merchant, 'verify');
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('dialog button')
    );
    buttons.find((b) => b.textContent?.includes('Approve shop'))?.click();
    http.expectOne('/api/v1/admin/merchants/merchant-1/verify').flush(null);
    await fixture.whenStable();
    http.expectOne((r) => r.url === '/api/v1/admin/merchants/unverified').flush(page([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No applications waiting');
  });
  it('shows customer management and requires a confirmation before disabling', () => {
    const fixture = TestBed.createComponent(AdminCustomersPage);
    http.expectOne((r) => r.url === '/api/v1/admin/customers').flush(page([customer]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('alex@example.test');
    fixture.componentInstance.target.set(customer);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog[open]')).toBeTruthy();
    http.expectNone((r) => r.method === 'PATCH');
  });
  it('rejects a blank category and submits trimmed, explicit category fields', async () => {
    const fixture = TestBed.createComponent(CategoryFormComponent);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    await c.submit();
    http.expectNone(() => true);
    expect(c.form.invalid).toBe(true);
    c.form.patchValue({ name: ' Stationery ', description: ' Paper ' });
    const request = c.submit();
    const r = http.expectOne('/api/v1/admin/categories');
    expect(r.request.body).toEqual({ name: 'Stationery', description: 'Paper' });
    r.flush(category);
    await request;
  });
  it('validates product price, stock integers and category before creating', async () => {
    const fixture = TestBed.createComponent(ProductFormComponent);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/v1/categories').flush(page([category]));
    const c = fixture.componentInstance;
    c.form.patchValue({ name: 'Notebook', priceCents: 0, stockQuantity: 1.5, categoryId: '' });
    await c.submit();
    http.expectNone('/api/v1/merchant/products');
    expect(c.form.controls.priceCents.invalid).toBe(true);
    expect(c.form.controls.stockQuantity.invalid).toBe(true);
    c.form.patchValue({ priceCents: 1299, stockQuantity: 4, categoryId: category.id });
    const request = c.submit();
    const r = http.expectOne('/api/v1/merchant/products');
    expect(r.request.body).not.toHaveProperty('active');
    r.flush(product);
    await request;
  });
  it('updates all required product fields, including activation', async () => {
    const fixture = TestBed.createComponent(ProductFormComponent);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/v1/categories').flush(page([category]));
    const c = fixture.componentInstance;
    c.form.controls.active.setValue(false);
    const request = c.submit();
    const r = http.expectOne('/api/v1/merchant/products/' + product.id);
    expect(r.request.method).toBe('PUT');
    expect(r.request.body).toMatchObject({
      name: product.name,
      categoryId: category.id,
      active: false,
    });
    r.flush({ ...product, active: false });
    await request;
  });
});
