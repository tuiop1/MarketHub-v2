import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartItem } from './data-access/cart-api.service';
import { MoneyPipe } from '../../shared/ui/money.pipe';
import { ProductArtComponent } from '../../shared/ui/product-art.component';
@Component({
  selector: 'mh-cart-item',
  imports: [RouterLink, MoneyPipe, ProductArtComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <article class="cart-item">
    <mh-product-art [seed]="item().productId" [compact]="true" />
    <div class="item-copy">
      <h2>
        <a [routerLink]="['/products', item().productId]">{{ item().productName }}</a>
      </h2>
      <p class="u-text-muted">{{ item().priceCents | money }} each</p>
      @if (unavailable()) {
        <p class="field-error">
          This item is unavailable or its quantity exceeds the current stock. Remove it to continue.
        </p>
      }
      <div class="actions">
        <span>Quantity: {{ item().quantity }}</span
        ><button
          class="button subtle"
          [disabled]="busy() || unavailable() || item().quantity >= item().stockQuantity"
          (click)="increase.emit()"
          [attr.aria-label]="'Add one more ' + item().productName"
        >
          Add one</button
        ><button
          class="button link"
          [disabled]="busy()"
          (click)="remove.emit()"
          [attr.aria-label]="'Remove ' + item().productName"
        >
          Remove
        </button>
      </div>
    </div>
    <strong class="u-money">{{ item().totalPriceCents | money }}</strong>
  </article>`,
  styleUrl: './cart-item.component.css',
})
export class CartItemComponent {
  readonly item = input.required<CartItem>();
  readonly busy = input(false);
  readonly increase = output<void>();
  readonly remove = output<void>();
  readonly unavailable = computed(
    () =>
      !this.item().productActive ||
      this.item().quantity > this.item().stockQuantity ||
      this.item().priceCents == null
  );
}
