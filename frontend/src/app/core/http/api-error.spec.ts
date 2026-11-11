import { HttpErrorResponse } from '@angular/common/http';
import { apiFailure } from './api-error';
describe('API error translation', () => {
  it.each([400, 401, 403, 404, 409, 429, 500, 502, 503, 0])(
    'maps HTTP %s without leaking diagnostics',
    (status) => {
      const result = apiFailure(
        new HttpErrorResponse({
          status,
          error: { message: 'java.lang.Exception: internal secrets' },
        })
      );
      expect(result.status).toBe(status);
      expect(result.message).not.toContain('java.lang');
      expect(result.message.length).toBeGreaterThan(12);
    }
  );
  it('distinguishes a missing cart from missing products and customers', () => {
    for (const resource of ['Product', 'CustomerResponse'])
      expect(
        apiFailure(
          new HttpErrorResponse({
            status: 404,
            error: { code: 'RESOURCE_NOT_FOUND', message: resource + ' was not found with id: x' },
          })
        ).missingCart
      ).toBe(false);
    expect(
      apiFailure(
        new HttpErrorResponse({
          status: 404,
          error: {
            code: 'RESOURCE_NOT_FOUND',
            message: 'Cart was not found with customerId: customer-1',
          },
        })
      ).missingCart
    ).toBe(true);
  });
  it('preserves validation paths with safe field feedback', () => {
    const result = apiFailure(
      new HttpErrorResponse({
        status: 400,
        error: { code: 'VALIDATION_FAILED', errors: { 'address.city': 'internal value' } },
      })
    );
    expect(result.fields['address.city']).toBe('Check this value.');
  });
  it('translates a known business conflict', () =>
    expect(
      apiFailure(new HttpErrorResponse({ status: 409, error: { code: 'EMAIL_ALREADY_TAKEN' } }))
        .message
    ).toContain('email address'));
});
