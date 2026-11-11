import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { IconComponent } from './icon.component';
@Component({
  selector: 'mh-product-art',
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div
    class="product-art"
    [class.compact]="compact()"
    [class.tone-sage]="tone() === 1"
    [class.tone-sand]="tone() === 2"
    [class.tone-rose]="tone() === 3"
    aria-hidden="true"
  >
    <mh-icon name="box" /><span>MarketHub</span>
  </div>`,
  styleUrl: './product-art.component.css',
})
export class ProductArtComponent {
  readonly seed = input('');
  readonly compact = input(false);
  readonly tone = computed(
    () => [...this.seed()].reduce((sum, letter) => sum + letter.charCodeAt(0), 0) % 4
  );
}
