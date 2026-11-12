import { HttpErrorResponse } from '@angular/common/http';
export class ApiFailure extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly code = '',
    readonly fields: Readonly<Record<string, string>> = {},
    readonly missingCart = false
  ) {
    super(message);
  }
}
const businessMessages: Readonly<Record<string, string>> = {
  EMAIL_ALREADY_TAKEN: 'An account already uses this email address.',
  MERCHANT_SHOP_NAME_ALREADY_TAKEN: 'This shop name is already in use.',
  CATEGORY_ALREADY_EXISTS: 'A category with this name already exists.',
  INSUFFICIENT_STOCK: 'There is not enough stock. Refresh to see the current availability.',
  EMPTY_CART: 'Your cart is empty. Add a product before checking out.',
  PRODUCT_NOT_AVAILABLE: 'This product is no longer available.',
  INACTIVE_PRODUCT: 'This product is no longer available.',
  INACTIVE_CATEGORY: 'This category is no longer available.',
  MERCHANT_INVALID_STATUS: 'Your shop must be approved before you can manage products.',
  MERCHANT_INVALID_STATUS_TRANSITION: 'This application has changed. Refresh before continuing.',
  MERCHANT_PENDING_VERIFICATION: 'This shop is waiting for approval.',
  MERCHANT_NOT_VERIFIED: 'This shop is not approved to sell.',
  MERCHANT_REJECTED: 'This shop is not approved to sell.',
  MERCHANT_SUSPENDED: 'This shop is currently suspended.',
};
const statusMessages: Readonly<Record<number, string>> = {
  0: 'Cannot reach MarketHub. Check your connection and try again.',
  400: 'Check the form values and try again.',
  401: 'Your session has ended. Sign in to continue.',
  403: 'Your account does not have access to this action.',
  404: 'This information is unavailable. It may have been removed or deactivated.',
  409: 'The information has changed. Refresh before trying again.',
  429: 'Too many requests. Wait a moment before trying again.',
  500: 'MarketHub could not complete this request. Try again later.',
  502: 'A MarketHub service is unavailable. Try again later.',
  503: 'MarketHub is temporarily unavailable. Try again later.',
};
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
export function apiFailure(error: unknown): ApiFailure {
  if (error instanceof ApiFailure) return error;
  if (!(error instanceof HttpErrorResponse))
    return new ApiFailure(0, 'This action could not be completed. Try again.');
  const body: unknown = error.error;
  const code = isRecord(body) && typeof body['code'] === 'string' ? body['code'] : '';
  const fields: Record<string, string> = {};
  if (isRecord(body) && isRecord(body['errors'])) {
    for (const key of Object.keys(body['errors'])) fields[key] = 'Check this value.';
  }
  // The backend reuses RESOURCE_NOT_FOUND for customers/products too; only this precise case is empty.
  const missingCart =
    error.status === 404 &&
    code === 'RESOURCE_NOT_FOUND' &&
    isRecord(body) &&
    typeof body['message'] === 'string' &&
    /^Cart was not found with customerId[:= ]/.test(body['message']);
  return new ApiFailure(
    error.status,
    businessMessages[code] ??
      statusMessages[error.status] ??
      'This request could not be completed. Try again later.',
    code,
    fields,
    missingCart
  );
}
