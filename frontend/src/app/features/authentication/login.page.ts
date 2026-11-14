import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService, safeReturnUrl } from '../../core/auth/auth.service';
import { IconComponent } from '../../shared/ui/icon.component';
@Component({
  imports: [RouterLink, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  readonly auth = inject(AuthService);
  readonly returnTo = safeReturnUrl(inject(ActivatedRoute).snapshot.queryParamMap.get('returnTo'));
}
