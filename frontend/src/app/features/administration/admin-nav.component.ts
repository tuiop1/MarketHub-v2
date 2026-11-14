import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
@Component({
  selector: 'mh-admin-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './admin-nav.component.html',
})
export class AdminNavComponent {}
