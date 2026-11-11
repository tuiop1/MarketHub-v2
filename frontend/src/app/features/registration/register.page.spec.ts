import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { RegisterPage } from './register.page';
import { customer } from '../../testing/fixtures';
describe('Registration form', () => {
  let http: HttpTestingController;
  const query = new BehaviorSubject(convertToParamMap({}));
  beforeEach(() => {
    query.next(convertToParamMap({}));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: query } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('starts empty and rejects blank identity fields before HTTP', async () => {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.firstName.value).toBe('');
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('This field is required');
    http.expectNone(() => true);
  });
  it('validates email, password length and a past birth date', () => {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    const form = fixture.componentInstance.form;
    form.patchValue({
      firstName: 'Alex',
      lastName: 'Test',
      email: 'invalid',
      password: 'short',
      customer: { birthDate: '2999-01-01' },
    });
    expect(form.controls.email.invalid).toBe(true);
    expect(form.controls.password.invalid).toBe(true);
    expect(form.controls.customer.controls.birthDate.hasError('past')).toBe(true);
  });
  it('requires shop information only for merchant registration', () => {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    const form = fixture.componentInstance.form;
    form.patchValue({
      firstName: 'Alex',
      lastName: 'Test',
      email: 'alex@example.test',
      password: 'test-password',
    });
    expect(form.valid).toBe(true);
    query.next(convertToParamMap({ type: 'merchant' }));
    fixture.detectChanges();
    expect(form.controls.customer.disabled).toBe(true);
    expect(form.controls.merchant.enabled).toBe(true);
    expect(form.valid).toBe(false);
    form.controls.merchant.controls.shopName.setValue('Paper shop');
    expect(form.valid).toBe(true);
  });
  it('submits only the customer contract and clears the password on success', async () => {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    c.form.patchValue({
      firstName: ' Alex ',
      lastName: 'Test',
      email: 'alex@example.test',
      password: 'test-password',
    });
    const first = c.submit();
    const second = c.submit();
    const request = http.expectOne('/api/v1/auth/customers/register');
    expect(request.request.body).toEqual({
      firstName: 'Alex',
      lastName: 'Test',
      email: 'alex@example.test',
      password: 'test-password',
      birthDate: null,
      address: null,
    });
    request.flush(customer);
    await Promise.all([first, second]);
    fixture.detectChanges();
    expect(c.form.controls.password.value).toBe('');
    expect(fixture.nativeElement.textContent).toContain('Your account is ready');
  });
  it('surfaces nested field validation errors without losing entered values', async () => {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    c.form.patchValue({
      firstName: 'Alex',
      lastName: 'Test',
      email: 'alex@example.test',
      password: 'test-password',
    });
    const saving = c.submit();
    http
      .expectOne('/api/v1/auth/customers/register')
      .flush(
        { errors: { 'address.city': 'must be valid' } },
        { status: 400, statusText: 'Bad Request' }
      );
    await saving;
    fixture.detectChanges();
    expect(c.form.controls.firstName.value).toBe('Alex');
    expect(fixture.nativeElement.textContent).toContain('Check this value');
  });
});
