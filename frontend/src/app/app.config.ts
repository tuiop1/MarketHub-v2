import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { authInterceptor } from './core/auth/auth.interceptor';
import { apiErrorInterceptor } from './core/http/api-error.interceptor';
import { diagnosticsInterceptor } from './core/diagnostics/diagnostics.interceptor';
import { environment } from '../environments/environment';
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        apiErrorInterceptor,
        ...(environment.diagnostics ? [diagnosticsInterceptor] : []),
      ])
    ),
    provideAppInitializer(() => inject(AuthService).initialize()),
  ],
};
