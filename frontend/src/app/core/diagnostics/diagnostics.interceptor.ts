import {
  HttpErrorResponse,
  HttpHeaders,
  HttpInterceptorFn,
  HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { defer, finalize, tap } from 'rxjs';
import { DiagnosticsService } from './diagnostics.service';

export const diagnosticsInterceptor: HttpInterceptorFn = (request, next) => {
  const diagnostics = inject(DiagnosticsService);
  if (!request.url.startsWith('/api/v1/')) return next(request);

  return defer(() => {
    const id = diagnostics.start(request.method, request.url);
    if (id === null) return next(request);
    const started = performance.now();
    return next(request).pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse)
            diagnostics.finish(id, event.status, performance.now() - started, event.headers);
        },
        error: (error: unknown) =>
          diagnostics.finish(
            id,
            error instanceof HttpErrorResponse ? error.status : 0,
            performance.now() - started,
            error instanceof HttpErrorResponse ? error.headers : new HttpHeaders()
          ),
      }),
      finalize(() => diagnostics.cancel(id, performance.now() - started))
    );
  });
};
