import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

type TokenClaims = {
  exp?: number;
  name?: string;
  preferred_username?: string;
  email?: string;
  realm_access?: { roles?: string[] };
};

type TokenResponse = {
  access_token: string;
  id_token?: string;
  expires_in: number;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = 'http://localhost:8090';
  private readonly realm = 'markethub';
  private readonly clientId = 'markethub-client';
  private readonly tokenKey = 'markethub_access_token';
  private readonly idTokenKey = 'markethub_id_token';
  private readonly verifierKey = 'markethub_pkce_verifier';
  private readonly stateKey = 'markethub_oauth_state';
  private expirationTimer: ReturnType<typeof setTimeout> | undefined;

  readonly accessToken = signal(sessionStorage.getItem(this.tokenKey));
  readonly claims = computed<TokenClaims | null>(() => this.decode(this.accessToken()));
  readonly roles = computed(() => this.claims()?.realm_access?.roles ?? []);
  readonly authenticated = computed(() => {
    const claims = this.claims();
    return !!claims && !!claims.exp && claims.exp * 1000 > Date.now();
  });
  readonly displayName = computed(() =>
    this.claims()?.name ?? this.claims()?.preferred_username ?? this.claims()?.email ?? 'User'
  );

  constructor(private readonly http: HttpClient) {
    // Remove credentials persisted by older frontend versions.
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.idTokenKey);
    this.scheduleExpiration(this.accessToken());
  }

  hasRole(role: string): boolean {
    return this.authenticated() && this.roles().includes(role);
  }

  async login(): Promise<void> {
    const verifier = this.randomString(64);
    const state = this.randomString(32);
    sessionStorage.setItem(this.verifierKey, verifier);
    sessionStorage.setItem(this.stateKey, state);
    const challenge = await this.sha256Base64Url(verifier);
    const redirectUri = `${location.origin}/callback`;
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: this.clientId,
      redirect_uri: redirectUri,
      scope: 'openid email profile',
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256'
    });
    location.href = `${this.keycloak}/realms/${this.realm}/protocol/openid-connect/auth?${params}`;
  }

  async completeLogin(code: string, returnedState: string | null): Promise<void> {
    const verifier = sessionStorage.getItem(this.verifierKey);
    const expectedState = sessionStorage.getItem(this.stateKey);
    if (!verifier) {
      this.clearLoginAttempt();
      throw new Error('PKCE verifier is missing. Start login again.');
    }
    if (!returnedState || !expectedState || returnedState !== expectedState) {
      this.clearLoginAttempt();
      throw new Error('OAuth state validation failed. Start login again.');
    }

    const body = new HttpParams()
      .set('grant_type', 'authorization_code')
      .set('client_id', this.clientId)
      .set('redirect_uri', `${location.origin}/callback`)
      .set('code', code)
      .set('code_verifier', verifier);
    const response = await firstValueFrom(this.http.post<TokenResponse>(
      `${this.keycloak}/realms/${this.realm}/protocol/openid-connect/token`,
      body,
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
    ));
    sessionStorage.setItem(this.tokenKey, response.access_token);
    if (response.id_token) sessionStorage.setItem(this.idTokenKey, response.id_token);
    this.accessToken.set(response.access_token);
    this.scheduleExpiration(response.access_token);
    this.clearLoginAttempt();
    history.replaceState({}, '', '/');
  }

  logout(): void {
    const idToken = sessionStorage.getItem(this.idTokenKey);
    const postLogoutRedirectUri = `${location.origin}/callback`;
    const params = new URLSearchParams({
      client_id: this.clientId,
      post_logout_redirect_uri: postLogoutRedirectUri
    });
    if (idToken) params.set('id_token_hint', idToken);

    this.clearLocalSession();
    location.href = `${this.keycloak}/realms/${this.realm}/protocol/openid-connect/logout?${params}`;
  }

  clearLocalSession(): void {
    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.idTokenKey);
    this.clearLoginAttempt();
    if (this.expirationTimer) clearTimeout(this.expirationTimer);
    this.expirationTimer = undefined;
    this.accessToken.set(null);
  }

  clearLoginAttempt(): void {
    sessionStorage.removeItem(this.verifierKey);
    sessionStorage.removeItem(this.stateKey);
  }

  private decode(token: string | null): TokenClaims | null {
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const bytes = Uint8Array.from(atob(padded), char => char.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes)) as TokenClaims;
    } catch {
      return null;
    }
  }

  private randomString(length: number): string {
    const bytes = crypto.getRandomValues(new Uint8Array(length));
    return this.base64Url(bytes);
  }

  private async sha256Base64Url(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    return this.base64Url(new Uint8Array(digest));
  }

  private base64Url(bytes: Uint8Array): string {
    let binary = '';
    bytes.forEach(byte => binary += String.fromCharCode(byte));
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  private scheduleExpiration(token: string | null): void {
    if (this.expirationTimer) clearTimeout(this.expirationTimer);
    this.expirationTimer = undefined;

    const expiration = this.decode(token)?.exp;
    if (!expiration) return;

    const delay = expiration * 1000 - Date.now();
    if (delay <= 0) {
      this.clearLocalSession();
      return;
    }

    this.expirationTimer = setTimeout(() => this.clearLocalSession(), delay);
  }
}
