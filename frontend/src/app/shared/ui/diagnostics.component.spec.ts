import { TestBed } from '@angular/core/testing';
import { HttpHeaders } from '@angular/common/http';
import {
  DIAGNOSTICS_AVAILABLE,
  DiagnosticsService,
} from '../../core/diagnostics/diagnostics.service';
import { DiagnosticsComponent } from './diagnostics.component';

describe('Diagnostics controls', () => {
  it('enables, collapses, clears and disables the panel through visible controls', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: DIAGNOSTICS_AVAILABLE, useValue: true }],
    });
    const fixture = TestBed.createComponent(DiagnosticsComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const toggle = element.querySelector<HTMLInputElement>('[role="switch"]')!;
    expect(element.querySelector('aside')).toBeNull();
    toggle.click();
    const service = TestBed.inject(DiagnosticsService);
    const id = service.start('GET', '/api/v1/products')!;
    service.finish(id, 200, 12, new HttpHeaders({ 'X-Cache': 'HIT' }));
    fixture.detectChanges();
    expect(element.textContent).toContain('HTTP 200');
    expect(element.textContent).toContain('Cache: Hit');
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
    toggle.click();
    fixture.detectChanges();
    expect(element.querySelector('aside')).toBeNull();
    expect(service.entries()).toEqual([]);
  });

  it('renders no switch when unavailable', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: DIAGNOSTICS_AVAILABLE, useValue: false }],
    });
    const fixture = TestBed.createComponent(DiagnosticsComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="switch"]')).toBeNull();
  });
});
