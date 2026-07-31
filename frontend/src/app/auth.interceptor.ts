import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const publicAuthEndpoint =
    request.url === '/api/v1/auth/customers/register' ||
    request.url === '/api/v1/auth/merchants/register';

  if (!request.url.startsWith('/api/') || publicAuthEndpoint) return next(request);
  if (!auth.authenticated()) {
    auth.clearLocalSession();
    return next(request);
  }

  const token = auth.accessToken();
  if (!token) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
