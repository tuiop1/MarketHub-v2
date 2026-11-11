import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
@Component({
  selector: 'mh-admin-nav',
  imports: [RouterLink, RouterLinkActive],
  template: `<nav class="tab-nav" aria-label="Administration">
    <a routerLink="/admin/categories" routerLinkActive="active">Categories</a
    ><a routerLink="/admin/merchants" routerLinkActive="active">Shops</a
    ><a routerLink="/admin/customers" routerLinkActive="active">Customers</a>
  </nav>`,
})
export class AdminNavComponent {}
