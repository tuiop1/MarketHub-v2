import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
const paths = {
  bag: 'M6 7h12l2 14H4L6 7ZM9 8V6a3 3 0 0 1 6 0v2',
  grid: 'M3 3h7v7H3V3Zm11 0h7v7h-7V3ZM3 14h7v7H3v-7Zm11 0h7v7h-7v-7Z',
  shop: 'M3 10h18L19 3H5l-2 7Zm2 0v11h14V10M9 21v-7h6v7M3 10c0 4 4 4 4 0 0 4 5 4 5 0 0 4 5 4 5 0 0 4 4 4 4 0',
  box: 'm12 3 9 5v9l-9 5-9-5V8l9-5Zm0 10v9M3 8l9 5 9-5M7 5.8l9 5',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  check: 'm5 12 4 4L19 6',
  user: 'M20 21v-2a7 7 0 0 0-14 0v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z',
  close: 'm6 6 12 12M6 18 18 6',
  menu: 'M3 6h18M3 12h18M3 18h18',
  shield: 'm12 3 8 3v6c0 5-8 10-8 10S4 17 4 12V6l8-3Zm-4 9 3 3 5-6',
  alert: 'm12 3 10 18H2L12 3Zm0 6v5m0 3v.5',
  minus: 'M5 12h14',
  plus: 'M5 12h14M12 5v14',
} as const;
@Component({
  selector: 'mh-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path [attr.d]="path()" />
  </svg>`,
  styleUrl: './icon.component.css',
})
export class IconComponent {
  readonly name = input<keyof typeof paths>('box');
  readonly path = computed(() => paths[this.name()]);
}
