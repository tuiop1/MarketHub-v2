import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CheckoutComponent } from './components/checkout.component';
import { CartPage } from '../cart/cart.page';
import { order, cart, product } from '../../testing/fixtures';
describe('Checkout workflow', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('blocks repeated clicks and reports a failed payment honestly', async () => {
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.componentRef.setInput('cart', true);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    c.payment.setValue('QR');
    const first = c.submit();
    const second = c.submit();
    const r = http.expectOne('/api/v1/orders/my-cart/purchase');
    r.flush({ ...order, status: 'PAYMENT_FAILED' });
    await Promise.all([first, second]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Payment failed');
    expect(fixture.nativeElement.textContent).not.toContain('Order paid');
    http.expectNone('/api/v1/carts/items');
  });
  it('blocks another purchase after an ambiguous failure', async () => {
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.componentRef.setInput('items', [{ productId: product.id, quantity: 1 }]);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    const purchase = c.submit();
    http.expectOne('/api/v1/orders/purchase').flush({}, { status: 503, statusText: 'Unavailable' });
    await purchase;
    await c.submit();
    http.expectNone('/api/v1/orders/purchase');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('outcome is uncertain');
    expect(fixture.nativeElement.querySelector('a[href="/orders"]')).toBeTruthy();
  });
  it('keeps a failed cart and does not clear or reload it', () => {
    const fixture = TestBed.createComponent(CartPage);
    http.expectOne('/api/v1/carts').flush(cart);
    fixture.detectChanges();
    fixture.componentInstance.onPlaced({ ...order, status: 'PAYMENT_FAILED' });
    expect(fixture.componentInstance.state().value).toEqual(cart);
    expect(fixture.nativeElement.textContent).toContain(product.name);
    expect(
      fixture.nativeElement.querySelector('mh-product-art, img, picture, [role="img"]')
    ).toBeNull();
    http.expectNone('/api/v1/carts');
    http.expectNone('/api/v1/carts/items');
  });
  it('reloads after PAID and keeps the receipt even when the cart is empty', async () => {
    const fixture = TestBed.createComponent(CartPage);
    http.expectOne('/api/v1/carts').flush(cart);
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button')
    );
    buttons.find((b) => b.textContent?.includes('Place simulated order'))?.click();
    http.expectOne('/api/v1/orders/my-cart/purchase').flush(order);
    await fixture.whenStable();
    http.expectOne('/api/v1/carts').flush({ ...cart, cartItems: [], totalPriceCents: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Order paid');
    expect(fixture.nativeElement.textContent).toContain('Your cart is empty');
  });
  it('prevents checkout when stock is below the cart quantity', () => {
    const fixture = TestBed.createComponent(CartPage);
    http
      .expectOne('/api/v1/carts')
      .flush({ ...cart, cartItems: [{ ...cart.cartItems[0], stockQuantity: 0 }] });
    fixture.detectChanges();
    expect(fixture.componentInstance.canCheckout()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('quantity exceeds');
  });
});
