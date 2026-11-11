import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ApiFailure } from '../../core/http/api-error';
@Component({
  selector: 'mh-feedback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (error(); as failure) {
      <div class="notice error" role="alert">
        <strong>Could not complete the request</strong>
        <p>{{ failure.message }}</p>
        @if (retryable()) {
          <button class="button subtle" (click)="retry.emit()">Try again</button>
        }
      </div>
    }
    @if (message()) {
      <p class="notice success" role="status">{{ message() }}</p>
    }`,
})
export class FeedbackComponent {
  readonly error = input<ApiFailure | null>(null);
  readonly message = input('');
  readonly retryable = input(false);
  readonly retry = output<void>();
}
@Component({
  selector: 'mh-loading',
  styleUrl: './loading.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="loading-state" role="status">
    <span class="loading-dot" aria-hidden="true"></span>{{ label() }}
  </div>`,
})
export class LoadingComponent {
  readonly label = input('Loading…');
}
@Component({
  selector: 'mh-empty',
  styleUrl: './empty.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="empty-state">
    <h2>{{ title() }}</h2>
    <p>{{ description() }}</p>
    <ng-content />
  </div>`,
})
export class EmptyComponent {
  readonly title = input.required<string>();
  readonly description = input('');
}
