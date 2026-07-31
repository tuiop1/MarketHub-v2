import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';
import { ApiService, Product } from './api.service';
import { AppComponent } from './app.component';
import { AuthService } from './auth.service';

describe('AppComponent', () => {
  it('treats a missing new-customer cart as an empty cart', async () => {
    const api = {
      cart: () => throwError(() => new HttpErrorResponse({ status: 404 }))
    } as Pick<ApiService, 'cart'>;
    const component = new AppComponent({} as AuthService, api as ApiService);

    await component.loadCart();

    expect(component.cart()).toBeNull();
    expect(component.result()).toBe('Ready.');
    expect(component.busy()).toBe(false);
  });

  it('clamps requested product quantities to available stock', () => {
    const component = new AppComponent({} as AuthService, {} as ApiService);
    const product = { id: 'product-1', stockQuantity: 3 } as Product;
    component.productQuantities[product.id] = 20;

    expect(component.quantity(product)).toBe(3);
  });
});
