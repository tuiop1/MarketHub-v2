import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
@Component({
  selector: 'mh-field-error',
  template: `<span class="field-error" [id]="errorId()" aria-live="polite">
    @if (control().touched && control().invalid) {
      @if (control().hasError('required') || control().hasError('blank')) {
        This field is required.
      } @else if (control().hasError('email')) {
        Enter a valid email address.
      } @else if (control().hasError('minlength')) {
        Use at least {{ control().getError('minlength').requiredLength }} characters.
      } @else if (control().hasError('maxlength')) {
        Use no more than {{ control().getError('maxlength').requiredLength }} characters.
      } @else if (control().hasError('past')) {
        Choose a date before today.
      } @else if (control().hasError('integer')) {
        Enter a whole number.
      } @else if (control().hasError('min')) {
        Enter at least {{ control().getError('min').min }}.
      } @else if (control().hasError('max')) {
        Enter no more than {{ control().getError('max').max }}.
      } @else {
        Check this value.
      }
    } @else if (serverError()) {
      {{ serverError() }}
    }
  </span>`,
})
export class FieldErrorComponent {
  readonly control = input.required<AbstractControl>();
  readonly errorId = input.required<string>();
  readonly serverError = input('');
}
