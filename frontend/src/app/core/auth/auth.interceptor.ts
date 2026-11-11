import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ApiFailure } from '../http/api-error';
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const registration = /^\/api\/v1\/auth\/(customers|merchants)\/register$/.test(request.url);
  if (!request.url.startsWith('/api/v1/') || registration || !auth.authenticated())
    return next(request);
  return from(auth.token()).pipe(
    switchMap((token) =>
      next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request)
    ),
    catchError((error: unknown) => {
      if (error instanceof ApiFailure && error.status === 401) auth.endSession();
      return throwError(() => error);
    })
  );
};
