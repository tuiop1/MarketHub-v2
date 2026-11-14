import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../http/api-error.interceptor';
import { ApiFailure } from '../http/api-error';
import { diagnosticsInterceptor } from './diagnostics.interceptor';
import { DIAGNOSTICS_AVAILABLE, DiagnosticsService } from './diagnostics.service';

describe('Opt-in API diagnostics', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let diagnostics: DiagnosticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor, diagnosticsInterceptor])),
        provideHttpClientTesting(),
        { provide: DIAGNOSTICS_AVAILABLE, useValue: true },
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    diagnostics = TestBed.inject(DiagnosticsService);
  });
  afterEach(() => http.verify());

  it('records nothing until enabled and never stores bodies, query values or request credentials', () => {
    client.get('/api/v1/products').subscribe();
    http.expectOne('/api/v1/products').flush([]);
    expect(diagnostics.entries()).toEqual([]);
    diagnostics.setEnabled(true);
    client
      .post(
        '/api/v1/auth/customers/register?email=private@example.test',
        { password: 'sensitive-body' },
        { headers: { Authorization: 'Bearer private-token' } }
      )
      .subscribe();
    http
      .expectOne('/api/v1/auth/customers/register?email=private@example.test')
      .flush({ secret: 'private-response' });
    expect(diagnostics.entries()[0].path).toBe('/api/v1/auth/customers/register');
    const stored = JSON.stringify(diagnostics.entries());
    for (const value of [
      'private@example.test',
      'sensitive-body',
      'private-token',
      'private-response',
    ])
      expect(stored).not.toContain(value);
  });

  it.each([
    ['HIT', 'Hit'],
    ['MISS', 'Miss'],
    [null, 'Not reported'],
  ])('reports only cache evidence from X-Cache (%s)', (header, expected) => {
    diagnostics.setEnabled(true);
    client.get('/api/v1/products').subscribe();
    expect(diagnostics.entries()[0].state).toBe('pending');
    http.expectOne('/api/v1/products').flush([], {
      headers: { 'X-Request-Id': 'request-123', ...(header ? { 'X-Cache': header } : {}) },
    });
    expect(diagnostics.entries()[0]).toMatchObject({
      state: 'complete',
      status: 200,
      cache: expected,
      requestId: 'request-123',
    });
    expect(diagnostics.entries()[0].durationMs).toBeGreaterThanOrEqual(0);
  });

  it('preserves application error handling and records raw failure headers', () => {
    diagnostics.setEnabled(true);
    const failed = vi.fn();
    client.get('/api/v1/products').subscribe({ error: failed });
    http.expectOne('/api/v1/products').flush(
      { message: 'internal detail' },
      {
        status: 503,
        statusText: 'Unavailable',
        headers: { 'X-Correlation-Id': 'failed-123' },
      }
    );
    expect(failed.mock.calls[0][0]).toBeInstanceOf(ApiFailure);
    expect(diagnostics.entries()[0]).toMatchObject({
      status: 503,
      requestId: 'failed-123',
      state: 'complete',
    });
    expect(JSON.stringify(diagnostics.entries())).not.toContain('internal detail');
  });

  it('marks network errors and cancellations accurately', () => {
    diagnostics.setEnabled(true);
    client.get('/api/v1/products').subscribe({ error: () => {} });
    http.expectOne('/api/v1/products').error(new ProgressEvent('error'));
    expect(diagnostics.entries()[0].status).toBe(0);
    const subscription = client.get('/api/v1/categories').subscribe();
    const request = http.expectOne('/api/v1/categories');
    subscription.unsubscribe();
    expect(request.cancelled).toBe(true);
    expect(diagnostics.entries()[0].state).toBe('cancelled');
  });

  it('does not restore cleared or disabled entries when in-flight requests finish', () => {
    diagnostics.setEnabled(true);
    client.get('/api/v1/products').subscribe();
    const first = http.expectOne('/api/v1/products');
    diagnostics.clear();
    client.get('/api/v1/categories').subscribe();
    const second = http.expectOne('/api/v1/categories');
    first.flush([]);
    expect(diagnostics.entries()).toHaveLength(1);
    expect(diagnostics.entries()[0].state).toBe('pending');
    diagnostics.setEnabled(false);
    diagnostics.setEnabled(true);
    second.flush([]);
    expect(diagnostics.entries()).toEqual([]);
  });

  it('keeps only the latest 25 requests and excludes external URLs', () => {
    diagnostics.setEnabled(true);
    for (let i = 0; i < 30; i++) {
      client.get(`/api/v1/products/${i}`).subscribe();
      http.expectOne(`/api/v1/products/${i}`).flush({});
    }
    client.get('https://other.test/api/v1/products').subscribe();
    http.expectOne('https://other.test/api/v1/products').flush([]);
    expect(diagnostics.entries()).toHaveLength(25);
    expect(diagnostics.entries().at(-1)?.path).toBe('/api/v1/products/5');
    expect(diagnostics.entries()[0].path).toBe('/api/v1/products/29');
  });

  it('cannot be enabled when unavailable in the production environment', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: DIAGNOSTICS_AVAILABLE, useValue: false }],
    });
    const unavailable = TestBed.inject(DiagnosticsService);
    unavailable.setEnabled(true);
    expect(unavailable.enabled()).toBe(false);
    expect(unavailable.start('GET', '/api/v1/products')).toBeNull();
  });
});
