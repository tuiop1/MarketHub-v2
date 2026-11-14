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
  templateUrl: './confirmation.component.html',
  styleUrl: './confirmation.component.scss',
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
