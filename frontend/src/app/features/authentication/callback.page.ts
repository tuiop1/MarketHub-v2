import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
@Component({
  imports: [RouterLink],
  template: `<section class="status-page">
    <h1>{{ auth.error() ? 'Let’s try sign-in again.' : 'Returning to MarketHub…' }}</h1>
    @if (auth.error()) {
      <p class="notice error" role="alert">{{ auth.error() }}</p>
      <a class="button primary" routerLink="/login">Sign in again</a>
    }
  </section>`,
})
export class CallbackPage implements OnInit {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  ngOnInit(): void {
    if (!this.auth.error())
      void this.router.navigateByUrl(
        this.auth.authenticated() ? this.auth.consumeReturnUrl() : '/products',
        { replaceUrl: true }
      );
  }
}
