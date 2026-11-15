import { TestBed } from '@angular/core/testing';
import { HttpHeaders } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { authStub, product } from '../../testing/fixtures';
import { AdminNavComponent } from '../../features/administration/admin-nav.component';
import { DiagnosticsService } from '../../core/diagnostics/diagnostics.service';
import { DiagnosticsComponent } from './diagnostics.component';

describe('Diagnostics controls', () => {
  it('enables, collapses, clears and disables the panel through visible controls', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub(['ADMIN']) }],
    });
    const fixture = TestBed.createComponent(DiagnosticsComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const navigation = TestBed.createComponent(AdminNavComponent);
    navigation.detectChanges();
    // Creating the second fixture detaches the first root; both must be mounted for focus checks.
    document.body.append(element);
    const toggle = navigation.nativeElement.querySelector('[role="switch"]') as HTMLInputElement;
    expect(element.querySelector('[role="switch"]')).toBeNull();
    expect(element.querySelector('aside')).toBeNull();
    toggle.click();
    navigation.detectChanges();
    const service = TestBed.inject(DiagnosticsService);
    const id = service.start('GET', `/api/v1/products/${product.id}`)!;
    service.finish(id, 200, 12, new HttpHeaders());
    fixture.detectChanges();
    expect(element.textContent).toContain('HTTP 200');
    expect(element.querySelector('.request-cache')?.textContent).toContain(
      'Caching policy: Eligible (up to 30 min)'
    );
    expect(element.textContent).toContain('not whether a response was cached');
    const collapse = element.querySelector<HTMLButtonElement>(
      '[aria-controls="diagnostics-body"]'
    )!;
    collapse.click();
    fixture.detectChanges();
    expect(element.querySelector<HTMLElement>('#diagnostics-body')!.hidden).toBe(true);
    collapse.click();
    fixture.detectChanges();
    const clear = Array.from(element.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === 'Clear'
    )!;
    clear.focus();
    clear.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(element.querySelector<HTMLElement>('#diagnostics-body')!.hidden).toBe(true);
    expect(document.activeElement).toBe(collapse);
    collapse.click();
    fixture.detectChanges();
    clear.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Earlier requests are not recorded');
    element.querySelector<HTMLButtonElement>('[aria-label="Turn off diagnostics"]')!.click();
    fixture.detectChanges();
    navigation.detectChanges();
    expect(document.activeElement).toBe(toggle);
    expect(toggle.checked).toBe(false);
    expect(element.querySelector('aside')).toBeNull();
    expect(service.entries()).toEqual([]);
  });

  it.each([{ roles: [] }, { roles: ['CUSTOMER'] }, { roles: ['MERCHANT'] }])(
    'hides the admin switch and panel for $roles',
    ({ roles }) => {
      TestBed.configureTestingModule({
        providers: [provideRouter([]), { provide: AuthService, useValue: authStub(roles) }],
      });
      const fixture = TestBed.createComponent(DiagnosticsComponent);
      fixture.detectChanges();
      const navigation = TestBed.createComponent(AdminNavComponent);
      navigation.detectChanges();
      TestBed.inject(DiagnosticsService).setEnabled(true);
      fixture.detectChanges();
      expect(navigation.nativeElement.querySelector('[role="switch"]')).toBeNull();
      expect(fixture.nativeElement.querySelector('aside')).toBeNull();
    }
  );

  it('keeps the enabled setting across admin pages and turns it off through the admin switch', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub(['ADMIN']) }],
    });
    const first = TestBed.createComponent(AdminNavComponent);
    first.detectChanges();
    first.nativeElement.querySelector('[role="switch"]').click();
    first.destroy();
    const second = TestBed.createComponent(AdminNavComponent);
    second.detectChanges();
    const toggle = second.nativeElement.querySelector('[role="switch"]') as HTMLInputElement;
    expect(toggle.checked).toBe(true);
    toggle.click();
    expect(TestBed.inject(DiagnosticsService).enabled()).toBe(false);
  });
});
