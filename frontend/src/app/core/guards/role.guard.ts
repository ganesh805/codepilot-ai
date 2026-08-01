import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard = (expectedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }

    const hasAnyRole = expectedRoles.some(role => authService.hasRole(role));
    if (hasAnyRole) {
      return true;
    }

    router.navigate(['/']);
    return false;
  };
};
