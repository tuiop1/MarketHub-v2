import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
@Component({
  selector: 'mh-field-error',
  templateUrl: './field-error.component.html',
})
export class FieldErrorComponent {
  readonly control = input.required<AbstractControl>();
  readonly errorId = input.required<string>();
  readonly serverError = input('');
}
