import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartItem } from './data-access/cart-api.service';
import { MoneyPipe } from '../../shared/ui/money.pipe';
@Component({
  selector: 'mh-cart-item',
  imports: [RouterLink, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cart-item.component.html',
  styleUrl: './cart-item.component.scss',
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
