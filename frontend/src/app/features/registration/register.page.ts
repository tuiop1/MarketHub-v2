import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AccountApi } from '../account/data-access/account-api.service';
import { ActionState } from '../../shared/state/request-state';
import { FeedbackComponent } from '../../shared/ui/feedback.component';
import { FieldErrorComponent } from '../../shared/ui/field-error.component';
import { nonBlank, pastDate } from '../../shared/utilities/validators';
@Component({
  imports: [ReactiveFormsModule, RouterLink, FeedbackComponent, FieldErrorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly api = inject(AccountApi);
  private readonly params = toSignal(inject(ActivatedRoute).queryParamMap);
  readonly merchant = computed(() => this.params()?.get('type') === 'merchant');
  readonly success = signal(false);
  readonly action = new ActionState(inject(DestroyRef));
  readonly form = this.fb.group({
    firstName: ['', [Validators.required, nonBlank, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, nonBlank, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    customer: this.fb.group({
      birthDate: ['', pastDate],
      country: ['', Validators.maxLength(100)],
      city: ['', Validators.maxLength(100)],
      street: ['', Validators.maxLength(255)],
      postalCode: ['', Validators.maxLength(32)],
      apartment: ['', Validators.maxLength(100)],
    }),
    merchant: this.fb.group({
      shopName: [
        '',
        [Validators.required, nonBlank, Validators.minLength(2), Validators.maxLength(120)],
      ],
      description: ['', Validators.maxLength(2000)],
    }),
  });
  constructor() {
    effect(() => {
      if (this.merchant()) {
        this.form.controls.customer.disable();
        this.form.controls.merchant.enable();
      } else {
        this.form.controls.merchant.disable();
        this.form.controls.customer.enable();
      }
      this.success.set(false);
      this.action.error.set(null);
      this.action.message.set('');
    });
  }
  async submit(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.action.busy()) return;
    const values = this.form.getRawValue();
    const identity = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim(),
      password: values.password,
    };
    if (this.merchant()) {
      await this.action.run(
        this.api.registerMerchant({
          ...identity,
          shopName: values.merchant.shopName.trim(),
          description: values.merchant.description.trim(),
        }),
        () => this.done()
      );
    } else {
      const c = values.customer;
      const address = {
        country: c.country.trim() || null,
        city: c.city.trim() || null,
        street: c.street.trim() || null,
        postalCode: c.postalCode.trim() || null,
        apartment: c.apartment.trim() || null,
      };
      await this.action.run(
        this.api.registerCustomer({
          ...identity,
          birthDate: c.birthDate || null,
          address: Object.values(address).some(Boolean) ? address : null,
        }),
        () => this.done()
      );
    }
  }
  private done(): void {
    this.form.reset();
    this.success.set(true);
  }
}
