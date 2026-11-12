import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './core/auth/auth.service';
import { authStub } from './testing/fixtures';
describe('Authentication-aware shell', () => {
  it.each([
    { roles: [], visible: 'Create account', hidden: 'My products' },
    { roles: ['CUSTOMER'], visible: 'Cart', hidden: 'Administration' },
    { roles: ['MERCHANT_PENDING'], visible: 'Account', hidden: 'My products' },
    { roles: ['MERCHANT'], visible: 'My products', hidden: 'Administration' },
    { roles: ['ADMIN'], visible: 'Administration', hidden: 'My products' },
  ])('shows the right navigation for $roles', ({ roles, visible, hidden }) => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub(roles) }],
    });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const nav = fixture.nativeElement.querySelector('nav').textContent;
    expect(nav).toContain(visible);
    expect(nav).not.toContain(hidden);
  });
  it('toggles the mobile menu through an accessible button', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub() }],
    });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.menu-toggle');
    expect(button.getAttribute('aria-expanded')).toBe('false');
    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
  });
});
