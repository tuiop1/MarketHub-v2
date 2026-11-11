import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
@Component({
  imports: [RouterLink],
  template: `<section class="status-page">
    <h1>{{ denied ? 'This area is restricted.' : 'This page is off the map.' }}</h1>
    <p>
      {{
        denied
          ? 'Your account does not have access to this area. You can check your account status or continue browsing.'
          : 'The address may have changed. Head back to the marketplace to find your next discovery.'
      }}
    </p>
    <div class="actions">
      <a class="button primary" routerLink="/products">Browse marketplace</a>
      @if (denied) {
        <a class="button subtle" routerLink="/account">View account</a>
      }
    </div>
  </section>`,
})
export class StatusPage {
  readonly denied = !!inject(ActivatedRoute).snapshot.data['denied'];
}
