import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
@Component({
  imports: [RouterLink],
  templateUrl: './callback.page.html',
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
