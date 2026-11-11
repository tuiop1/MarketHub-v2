import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
export const nonBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;
export const integer: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.value === null || control.value === '' || Number.isSafeInteger(control.value)
    ? null
    : { integer: true };
export const pastDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  if (!control.value) return null;
  const today = new Date();
  const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  return /^\d{4}-\d{2}-\d{2}$/.test(control.value) && control.value < localDate
    ? null
    : { past: true };
};
