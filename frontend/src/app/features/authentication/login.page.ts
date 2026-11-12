import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService, safeReturnUrl } from '../../core/auth/auth.service';
import { IconComponent } from '../../shared/ui/icon.component';
@Component({
  imports: [RouterLink, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <section class="sign-in panel">
    <mh-icon name="user" />
    <h1>Welcome back.</h1>
    <p class="u-text-muted">Sign in to manage your account, shop, or next good find.</p>
    @if (auth.error()) {
      <p class="notice error" role="alert">{{ auth.error() }}</p>
    }
    @if (auth.sessionEnded()) {
      <p class="notice warning" role="status">Your session has ended. Sign in again to continue.</p>
    }
    <button class="button primary" (click)="auth.login(returnTo)">Continue to sign in</button>
    <p class="u-text-small u-text-muted sign-in-note">
      You’ll sign in securely through MarketHub’s identity provider.
    </p>
    <p>New here? <a routerLink="/register">Create an account</a></p>
    <a routerLink="/products">Continue browsing</a>
  </section>`,
  styleUrl: './login.page.css',
})
export class LoginPage {
  readonly auth = inject(AuthService);
  readonly returnTo = safeReturnUrl(inject(ActivatedRoute).snapshot.queryParamMap.get('returnTo'));
}
