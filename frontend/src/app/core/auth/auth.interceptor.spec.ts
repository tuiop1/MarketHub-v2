import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { apiErrorInterceptor } from '../http/api-error.interceptor';
import { AuthService } from './auth.service';
import { authStub } from '../../testing/fixtures';
describe('Authentication interceptor', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let auth: ReturnType<typeof authStub>;
  beforeEach(() => {
    auth = authStub(['CUSTOMER']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });
  afterEach(() => http.verify());
  it('renews before attaching a bearer token to a gateway request', async () => {
    client.get('/api/v1/carts').subscribe();
    await Promise.resolve();
    const r = http.expectOne('/api/v1/carts');
    expect(r.request.headers.get('Authorization')).toBe('Bearer access-token');
    expect(auth.token).toHaveBeenCalledOnce();
    r.flush({});
  });
  it.each([
    'https://other.test/api/v1/carts',
    'http://localhost:8090/realms/markethub/protocol/openid-connect/token',
    '/api/v1/auth/customers/register',
    '/api/v1/auth/merchants/register',
  ])('never leaks tokens to %s', (url) => {
    client.post(url, {}).subscribe();
    const r = http.expectOne(url);
    expect(r.request.headers.has('Authorization')).toBe(false);
    expect(auth.token).not.toHaveBeenCalled();
    r.flush({});
  });
  it('ends a rejected session without retrying the mutation', async () => {
    client.post('/api/v1/orders/purchase', {}).subscribe({ error: () => {} });
    await Promise.resolve();
    http
      .expectOne('/api/v1/orders/purchase')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.endSession).toHaveBeenCalledOnce();
    http.expectNone('/api/v1/orders/purchase');
  });
});
