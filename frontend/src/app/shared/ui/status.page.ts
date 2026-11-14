import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
@Component({
  imports: [RouterLink],
  templateUrl: './status.page.html',
})
export class StatusPage {
  readonly denied = !!inject(ActivatedRoute).snapshot.data['denied'];
}
