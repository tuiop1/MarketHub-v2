import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { AUTH_CLIENT, AuthService, safeReturnUrl } from './auth.service';
const webcrypto = process.getBuiltinModule('crypto').webcrypto;
function jwt(claims: object): string {
  const encode = (v: object) =>
    btoa(JSON.stringify(v)).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
  return encode({ alg: 'RS256' }) + '.' + encode(claims) + '.signature';
}
function client() {
  return new Keycloak({
    url: 'http://localhost:8090',
    realm: 'markethub',
    clientId: 'markethub-client',
  });
}
describe('Keycloak adapter integration', () => {
  let adapter: Keycloak;
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    history.replaceState({}, '', '/products');
    vi.stubGlobal('crypto', webcrypto);
    vi.stubGlobal('isSecureContext', true);
    adapter = client();
    TestBed.configureTestingModule({ providers: [{ provide: AUTH_CLIENT, useValue: adapter }] });
  });
  afterEach(() => {
    adapter.clearToken();
    vi.unstubAllGlobals();
    vi.useRealTimers();
    history.replaceState({}, '', '/');
  });
  it('initializes code flow with PKCE and keeps anonymous browsing network-free', async () => {
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    const service = TestBed.inject(AuthService);
    await service.initialize();
    expect(service.authenticated()).toBe(false);
    expect(adapter.flow).toBe('standard');
    expect(fetch).not.toHaveBeenCalled();
    const url = new URL(
      await adapter.createLoginUrl({ redirectUri: location.origin + '/callback' })
    );
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('state')).toBeTruthy();
    expect(url.searchParams.get('nonce')).toBeTruthy();
  });
  it('rejects unknown OAuth state without exchanging the code', async () => {
    history.replaceState({}, '', '/callback?code=untrusted&state=unknown');
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    const service = TestBed.inject(AuthService);
    await service.initialize();
    expect(service.authenticated()).toBe(false);
    expect(service.error()).toContain('Sign-in could not');
    expect(fetch).not.toHaveBeenCalled();
    expect(location.search).toBe('');
  });
  it('exchanges a state-bound callback with its verifier and stores no tokens', async () => {
    const first = client();
    await first.init({ checkLoginIframe: false, pkceMethod: 'S256', responseMode: 'query' });
    const url = new URL(await first.createLoginUrl({ redirectUri: location.origin + '/callback' }));
    const state = url.searchParams.get('state');
    const nonce = url.searchParams.get('nonce');
    const exp = Math.floor(Date.now() / 1000) + 300;
    history.replaceState({}, '', '/callback?code=valid-code&state=' + state);
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          access_token: jwt({
            exp,
            iat: exp - 300,
            realm_access: { roles: ['CUSTOMER'] },
            name: 'Alex',
          }),
          id_token: jwt({ exp, nonce }),
          refresh_token: jwt({ exp: exp + 300 }),
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    );
    vi.stubGlobal('fetch', fetch);
    const service = TestBed.inject(AuthService);
    await service.initialize();
    expect(service.authenticated()).toBe(true);
    expect(service.hasRole('CUSTOMER')).toBe(true);
    expect(service.hasRole('ADMIN')).toBe(false);
    expect(fetch).toHaveBeenCalledTimes(1);
    const body = fetch.mock.calls[0][1].body;
    expect(body.get('code')).toBe('valid-code');
    expect(body.get('code_verifier')).toBeTruthy();
    expect(body.get('grant_type')).toBe('authorization_code');
    expect(
      [...Object.values(localStorage), ...Object.values(sessionStorage)].some(
        (v) => v.includes('access_token') || v.includes('refresh_token')
      )
    ).toBe(false);
  });
  it('rejects a nonce mismatch', async () => {
    const first = client();
    await first.init({ checkLoginIframe: false, pkceMethod: 'S256', responseMode: 'query' });
    const url = new URL(await first.createLoginUrl({ redirectUri: location.origin + '/callback' }));
    history.replaceState({}, '', '/callback?code=code&state=' + url.searchParams.get('state'));
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            access_token: jwt({
              exp: Math.floor(Date.now() / 1000) + 60,
              iat: Math.floor(Date.now() / 1000),
            }),
            id_token: jwt({ exp: Math.floor(Date.now() / 1000) + 60, nonce: 'wrong' }),
          }),
          { status: 200 }
        )
      )
    );
    const service = TestBed.inject(AuthService);
    await service.initialize();
    expect(service.authenticated()).toBe(false);
    expect(service.error()).toBeTruthy();
  });
  it('clears credentials when refresh fails after expiration', async () => {
    const service = TestBed.inject(AuthService);
    await service.initialize();
    adapter.authenticated = true;
    adapter.token = 'expired';
    vi.spyOn(adapter, 'updateToken').mockRejectedValue(new Error('expired'));
    adapter.onTokenExpired?.();
    await vi.waitFor(() => expect(adapter.token).toBeUndefined());
    expect(service.authenticated()).toBe(false);
    expect(adapter.token).toBeUndefined();
    expect(service.sessionEnded()).toBe(true);
  });
  it('builds a logout URL for the configured callback without a client secret', async () => {
    await TestBed.inject(AuthService).initialize();
    const url = new URL(adapter.createLogoutUrl({ redirectUri: location.origin + '/callback' }));
    expect(url.pathname).toBe('/realms/markethub/protocol/openid-connect/logout');
    expect(url.searchParams.get('post_logout_redirect_uri')).toBe(location.origin + '/callback');
    expect(url.searchParams.has('client_secret')).toBe(false);
  });
  it.each([
    'https://evil.test',
    '//evil.test',
    '/\\evil.test',
    '/callback',
    '/login',
    '/products\n',
  ])('rejects unsafe return target %s', (value) => expect(safeReturnUrl(value)).toBe('/products'));
  it('preserves an internal deep link', () =>
    expect(safeReturnUrl('/products/x?page=1')).toBe('/products/x?page=1'));
});
