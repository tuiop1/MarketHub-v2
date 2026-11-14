import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ApiFailure } from '../../core/http/api-error';
@Component({
  selector: 'mh-feedback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './feedback.component.html',
})
export class FeedbackComponent {
  readonly error = input<ApiFailure | null>(null);
  readonly message = input('');
  readonly retryable = input(false);
  readonly retry = output<void>();
}
@Component({
  selector: 'mh-loading',
  styleUrl: './loading.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loading.component.html',
})
export class LoadingComponent {
  readonly label = input('Loading…');
}
@Component({
  selector: 'mh-empty',
  styleUrl: './empty.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './empty.component.html',
})
export class EmptyComponent {
  readonly title = input.required<string>();
  readonly description = input('');
}
