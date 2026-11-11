import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Component } from '@angular/core';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { authStub } from '../../testing/fixtures';
@Component({ selector: 'test-protected', template: 'Protected' })
class ProtectedPage {}
@Component({ selector: 'test-sign-in', template: 'Sign in' })
class SignInPage {}
@Component({ selector: 'test-denied', template: 'Restricted' })
class DeniedPage {}
describe('Role-aware routing', () => {
  it.each(
    [[], ['MERCHANT_PENDING'], ['MERCHANT_REJECTED'], ['CUSTOMER'], ['MERCHANT']].map((roles) => ({
      roles,
    }))
  )('protects merchant product management for roles %j', async ({ roles }) => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authStub(roles) },
        provideRouter([
          {
            path: 'merchant/products',
            component: ProtectedPage,
            canActivate: [authGuard],
            data: { roles: ['MERCHANT'] },
          },
          { path: 'login', component: SignInPage },
          { path: 'access-denied', component: DeniedPage },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/merchant/products');
    const url = TestBed.inject(Router).url;
    if (!roles.length) expect(url).toContain('/login?returnTo=');
    else if (roles.includes('MERCHANT'))
      expect(harness.routeNativeElement?.textContent).toContain('Protected');
    else expect(url).toBe('/access-denied');
  });
});
