import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, of, startWith, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AccountApi } from './data-access/account-api.service';
import { requestState, loading } from '../../shared/state/request-state';
import { Customer, Merchant } from './data-access/account.models';
import { FeedbackComponent, LoadingComponent } from '../../shared/ui/feedback.component';
@Component({
  imports: [DatePipe, RouterLink, FeedbackComponent, LoadingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account.page.html',
  styleUrl: './account.page.scss',
})
export class AccountPage {
  readonly auth = inject(AuthService);
  private readonly api = inject(AccountApi);
  readonly reload = new Subject<void>();
  readonly customer = toSignal(
    this.reload.pipe(
      startWith(undefined),
      switchMap(() =>
        requestState(this.auth.hasRole('CUSTOMER') ? this.api.customerMe() : of(null))
      )
    ),
    { initialValue: loading<Customer | null>() }
  );
  readonly merchant = toSignal(
    this.reload.pipe(
      startWith(undefined),
      switchMap(() => requestState(this.auth.isMerchant() ? this.api.merchantMe() : of(null)))
    ),
    { initialValue: loading<Merchant | null>() }
  );
}
