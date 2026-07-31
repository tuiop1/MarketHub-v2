import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

const token = (claims: object): string => {
  const encode = (value: object) => btoa(JSON.stringify(value))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `${encode({ alg: 'none' })}.${encode(claims)}.`;
};

describe('AuthService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.inject(AuthService).clearLocalSession();
    http.verify();
    vi.useRealTimers();
  });

  it('restores a valid session token and its roles', () => {
    sessionStorage.setItem('markethub_access_token', token({
      exp: Math.floor(Date.now() / 1000) + 60,
      realm_access: { roles: ['CUSTOMER'] }
    }));

    const service = TestBed.inject(AuthService);

    expect(service.authenticated()).toBe(true);
    expect(service.hasRole('CUSTOMER')).toBe(true);
  });

  it('clears the session when the access token expires', () => {
    vi.useFakeTimers();
    const now = new Date('2026-07-31T12:00:00Z');
    vi.setSystemTime(now);
    sessionStorage.setItem('markethub_access_token', token({
      exp: Math.floor(now.getTime() / 1000) + 1
    }));
    const service = TestBed.inject(AuthService);

    vi.advanceTimersByTime(1_001);

    expect(service.authenticated()).toBe(false);
    expect(sessionStorage.getItem('markethub_access_token')).toBeNull();
  });

  it('rejects a callback whose OAuth state does not match', async () => {
    sessionStorage.setItem('markethub_pkce_verifier', 'verifier');
    sessionStorage.setItem('markethub_oauth_state', 'expected');
    const service = TestBed.inject(AuthService);

    await expect(service.completeLogin('code', 'different')).rejects.toThrow('OAuth state validation failed');
    expect(sessionStorage.getItem('markethub_pkce_verifier')).toBeNull();
    expect(sessionStorage.getItem('markethub_oauth_state')).toBeNull();
  });

  it('exchanges a valid callback and keeps tokens only in session storage', async () => {
    sessionStorage.setItem('markethub_pkce_verifier', 'verifier');
    sessionStorage.setItem('markethub_oauth_state', 'expected');
    const service = TestBed.inject(AuthService);
    const accessToken = token({ exp: Math.floor(Date.now() / 1000) + 60 });

    const completion = service.completeLogin('code', 'expected');
    const request = http.expectOne('http://localhost:8090/realms/markethub/protocol/openid-connect/token');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('code')).toBe('code');
    expect(request.request.body.get('code_verifier')).toBe('verifier');
    request.flush({ access_token: accessToken, id_token: 'id-token', expires_in: 60 });
    await completion;

    expect(sessionStorage.getItem('markethub_access_token')).toBe(accessToken);
    expect(localStorage.getItem('markethub_access_token')).toBeNull();
  });
});
