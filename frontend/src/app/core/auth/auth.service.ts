import { Injectable, InjectionToken, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';
export type Role = 'ADMIN' | 'CUSTOMER' | 'MERCHANT_PENDING' | 'MERCHANT' | 'MERCHANT_REJECTED';
export const AUTH_CLIENT = new InjectionToken<Keycloak>('Keycloak browser adapter', {
  providedIn: 'root',
  factory: () =>
    new Keycloak({
      url: environment.keycloakUrl,
      realm: 'markethub',
      clientId: 'markethub-client',
    }),
});
export function safeReturnUrl(value: string | null): string {
  if (!value || !value.startsWith('/') || value.startsWith('//') || /[\\\r\n]/.test(value))
    return '/products';
  const path = value.split(/[?#]/)[0];
  return ['/callback', '/login'].includes(path) ? '/products' : value;
}
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly client = inject(AUTH_CLIENT);
  private readonly returnKey = 'markethub.returnTo';
  readonly authenticated = signal(false);
  readonly roles = signal<readonly string[]>([]);
  readonly displayName = signal('Your account');
  readonly error = signal('');
  readonly sessionEnded = signal(false);
  readonly isMerchant = computed(() =>
    this.roles().some((role) =>
      ['MERCHANT', 'MERCHANT_PENDING', 'MERCHANT_REJECTED'].includes(role)
    )
  );
  private initialization: Promise<void> | undefined;
  initialize(): Promise<void> {
    return (this.initialization ??= this.initializeClient());
  }
  private async initializeClient(): Promise<void> {
    // Remove credentials retained by the previous client. New tokens live only in adapter memory.
    for (const key of [
      'markethub_access_token',
      'markethub_id_token',
      'markethub_pkce_verifier',
      'markethub_oauth_state',
    ]) {
      sessionStorage.removeItem(key);
      localStorage.removeItem(key);
    }
    this.client.onAuthSuccess = () => this.sync();
    this.client.onAuthRefreshSuccess = () => this.sync();
    this.client.onAuthLogout = () => this.endSession();
    this.client.onAuthRefreshError = () => this.endSession();
    this.client.onTokenExpired = () => {
      void this.token().catch(() => this.endSession());
    };
    const callbackParams = new URLSearchParams(location.search);
    const hadCallback =
      location.pathname === '/callback' &&
      ['code', 'state', 'error'].some((key) => callbackParams.has(key));
    try {
      await this.client.init({
        flow: 'standard',
        pkceMethod: 'S256',
        responseMode: 'query',
        redirectUri: `${location.origin}/callback`,
        checkLoginIframe: false,
        enableLogging: false,
      });
      this.sync();
      if (hadCallback && !this.authenticated()) {
        this.error.set('Sign-in could not be completed. Start sign-in again.');
        history.replaceState({}, '', '/callback');
      }
    } catch {
      this.client.clearToken();
      this.sync();
      this.error.set('Sign-in could not be completed. Start sign-in again.');
      // Remove authorization codes/error descriptions from the address bar, even on a failed callback.
      if (location.pathname === '/callback') history.replaceState({}, '', '/callback');
    }
  }
  hasRole(role: Role): boolean {
    return this.authenticated() && this.roles().includes(role);
  }
  async login(returnTo = '/products'): Promise<void> {
    this.error.set('');
    this.sessionEnded.set(false);
    sessionStorage.setItem(this.returnKey, safeReturnUrl(returnTo));
    try {
      await this.client.login({ redirectUri: `${location.origin}/callback` });
    } catch {
      this.error.set('Cannot open sign-in. Check your connection and try again.');
    }
  }
  consumeReturnUrl(): string {
    const route = safeReturnUrl(sessionStorage.getItem(this.returnKey));
    sessionStorage.removeItem(this.returnKey);
    return route;
  }
  async logout(): Promise<void> {
    this.error.set('');
    try {
      await this.client.logout({ redirectUri: `${location.origin}/callback` });
    } catch {
      this.endSession();
      this.error.set('Signed out of MarketHub. The identity provider could not be reached.');
    }
  }
  async token(): Promise<string | null> {
    if (!this.client.authenticated) return null;
    try {
      await this.client.updateToken(30);
      this.sync();
      return this.client.token ?? null;
    } catch {
      this.endSession();
      throw new Error('Session expired');
    }
  }
  endSession(): void {
    if (this.client.token) this.client.clearToken();
    this.authenticated.set(false);
    this.roles.set([]);
    this.sessionEnded.set(true);
  }
  private sync(): void {
    this.authenticated.set(!!this.client.authenticated);
    this.roles.set(this.client.realmAccess?.roles ?? []);
    const name: unknown = this.client.tokenParsed?.['name'];
    const username: unknown = this.client.tokenParsed?.['preferred_username'];
    this.displayName.set(
      typeof name === 'string' ? name : typeof username === 'string' ? username : 'Your account'
    );
  }
}
