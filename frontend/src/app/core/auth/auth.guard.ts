import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.authenticated())
    return router.createUrlTree(['/login'], { queryParams: { returnTo: state.url } });
  const required: unknown = route.data['roles'];
  if (
    Array.isArray(required) &&
    !required.some((role) => typeof role === 'string' && auth.roles().includes(role))
  ) {
    return router.createUrlTree(['/access-denied']);
  }
  return true;
};
