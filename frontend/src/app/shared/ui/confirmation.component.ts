import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  input,
  output,
  viewChild,
} from '@angular/core';
@Component({
  selector: 'mh-confirmation',
  template: `<dialog
    #dialog
    aria-labelledby="confirm-title"
    aria-describedby="confirm-message"
    (cancel)="cancel($event)"
  >
    <h2 id="confirm-title">{{ title() }}</h2>
    <p id="confirm-message">{{ message() }}</p>
    <div class="actions">
      <button class="button subtle" autofocus (click)="cancelled.emit()">Cancel</button>
      <button class="button danger" (click)="confirmed.emit()">{{ confirmLabel() }}</button>
    </div>
  </dialog>`,
  styleUrl: './confirmation.component.css',
})
export class ConfirmationComponent implements AfterViewInit, OnDestroy {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Confirm');
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  ngAfterViewInit(): void {
    this.dialog().nativeElement.showModal();
  }
  ngOnDestroy(): void {
    this.dialog().nativeElement.close();
  }
  cancel(event: Event): void {
    event.preventDefault();
    this.cancelled.emit();
  }
}
