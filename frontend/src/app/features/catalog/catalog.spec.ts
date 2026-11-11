import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ProductListPage } from './pages/product-list.page';
import { ProductCardComponent } from './components/product-card.component';
import { AuthService } from '../../core/auth/auth.service';
import { product, page, authStub, cartItem } from '../../testing/fixtures';
describe('Catalog UI', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'products', component: ProductListPage }]),
        { provide: AuthService, useValue: authStub() },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it('renders a loading state then actual product data', () => {
    const fixture = TestBed.createComponent(ProductListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Finding products');
    http.expectOne((r) => r.url === '/api/v1/products').flush(page([product]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Field notebook');
    expect(fixture.nativeElement.textContent).toContain('$12.99');
    expect(fixture.nativeElement.querySelector('mh-product-card')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Add to cart');
  });
  it('renders an empty state with a useful next action', () => {
    const fixture = TestBed.createComponent(ProductListPage);
    http.expectOne((r) => r.url === '/api/v1/products').flush(page([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No products here yet');
    expect(fixture.nativeElement.querySelector('a[href="/merchants"]')).toBeTruthy();
  });
  it('shows a safe error and supports retry', () => {
    const fixture = TestBed.createComponent(ProductListPage);
    http
      .expectOne((r) => r.url === '/api/v1/products')
      .flush({ message: 'stacktrace' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('temporarily unavailable');
    expect(fixture.nativeElement.textContent).not.toContain('stacktrace');
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button')
    );
    buttons.find((b) => b.textContent?.includes('Try again'))?.click();
    http.expectOne((r) => r.url === '/api/v1/products').flush(page([product]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(product.name);
  });
  it('cancels stale pages when query parameters change', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/products?page=0', ProductListPage);
    const old = http.expectOne((r) => r.url === '/api/v1/products');
    await harness.navigateByUrl('/products?page=2&sort=priceCents,desc', ProductListPage);
    expect(old.cancelled).toBe(true);
    const current = http.expectOne((r) => r.url === '/api/v1/products');
    expect(current.request.params.get('page')).toBe('2');
    expect(current.request.params.get('sort')).toBe('priceCents,desc');
    current.flush(page([product], 2, 4));
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Page 3 of 4');
  });
  it('prevents invalid and duplicate card submissions', async () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.componentRef.setInput('customer', true);
    fixture.detectChanges();
    const card = fixture.componentInstance;
    card.quantity.setValue(9);
    await card.add();
    http.expectNone('/api/v1/carts/items');
    card.quantity.setValue(2);
    const first = card.add();
    const second = card.add();
    const request = http.expectOne('/api/v1/carts/items');
    expect(request.request.body.quantity).toBe(2);
    request.flush(cartItem);
    await Promise.all([first, second]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Added to your cart');
  });
  it('emits selected product IDs and validated quantities for batch checkout', () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.componentRef.setInput('customer', true);
    fixture.detectChanges();
    const select = vi.fn();
    fixture.componentInstance.selection.subscribe(select);
    fixture.componentInstance.quantity.setValue(3);
    const checkbox: HTMLInputElement = fixture.nativeElement.querySelector('input[type=checkbox]');
    checkbox.click();
    expect(select).toHaveBeenCalledWith({ productId: product.id, quantity: 3 });
  });
});
