import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MerchantProductsApi } from './data-access/merchant-products-api.service';
import { Product } from '../catalog/data-access/catalog.models';
import { AccountApi } from '../account/data-access/account-api.service';
import { ProductFormComponent } from './product-form.component';
import { ActionState, requestState } from '../../shared/state/request-state';
import { pagedResource } from '../../shared/state/paged-resource';
import {
  FeedbackComponent,
  LoadingComponent,
  EmptyComponent,
} from '../../shared/ui/feedback.component';
import { ConfirmationComponent } from '../../shared/ui/confirmation.component';
import { PaginationComponent } from '../../shared/ui/pagination.component';
import { MoneyPipe } from '../../shared/ui/money.pipe';
@Component({
  imports: [
    ProductFormComponent,
    FeedbackComponent,
    LoadingComponent,
    EmptyComponent,
    ConfirmationComponent,
    PaginationComponent,
    MoneyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './products.page.html',
  styleUrl: './products.page.scss',
})
export class MerchantProductsPage {
  private readonly api = inject(MerchantProductsApi);
  readonly profile = toSignal(requestState(inject(AccountApi).merchantMe()));
  readonly resource = pagedResource((page, sort) => this.api.list(page, sort), ['createdAt,desc']);
  readonly formBusy = signal(false);
  readonly action = new ActionState(inject(DestroyRef));
  readonly editing = signal<Product | null>(null);
  readonly deactivating = signal<Product | null>(null);
  saved(): void {
    this.editing.set(null);
    this.action.message.set('Product saved.');
    this.resource.reload();
  }
  async deactivate(product: Product): Promise<void> {
    this.deactivating.set(null);
    await this.action.run(this.api.deactivate(product.id), () => {
      this.action.message.set('Product deactivated.');
      if (this.editing()?.id === product.id) this.editing.set(null);
      this.resource.reload();
    });
  }
}
