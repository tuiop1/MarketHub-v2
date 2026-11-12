import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  {
    path: 'products',
    title: 'Marketplace · MarketHub',
    loadComponent: () =>
      import('./features/catalog/pages/product-list.page').then((m) => m.ProductListPage),
  },
  {
    path: 'products/:id',
    title: 'Product · MarketHub',
    loadComponent: () =>
      import('./features/catalog/pages/product-details.page').then((m) => m.ProductDetailsPage),
  },
  {
    path: 'categories',
    title: 'Categories · MarketHub',
    loadComponent: () =>
      import('./features/catalog/pages/categories.page').then((m) => m.CategoriesPage),
  },
  {
    path: 'categories/:id',
    title: 'Category · MarketHub',
    loadComponent: () =>
      import('./features/catalog/pages/category-details.page').then((m) => m.CategoryDetailsPage),
  },
  {
    path: 'merchants',
    title: 'Shops · MarketHub',
    loadComponent: () =>
      import('./features/merchants/pages/merchants.page').then((m) => m.MerchantsPage),
  },
  {
    path: 'merchants/:id',
    title: 'Shop · MarketHub',
    loadComponent: () =>
      import('./features/merchants/pages/merchant-details.page').then((m) => m.MerchantDetailsPage),
  },
  {
    path: 'register',
    title: 'Create account · MarketHub',
    loadComponent: () =>
      import('./features/registration/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'login',
    title: 'Sign in · MarketHub',
    loadComponent: () => import('./features/authentication/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'callback',
    title: 'Sign-in · MarketHub',
    loadComponent: () =>
      import('./features/authentication/callback.page').then((m) => m.CallbackPage),
  },
  {
    path: 'account',
    title: 'Your account · MarketHub',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account.page').then((m) => m.AccountPage),
  },
  {
    path: 'cart',
    title: 'Your cart · MarketHub',
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER'] },
    loadComponent: () => import('./features/cart/cart.page').then((m) => m.CartPage),
  },
  {
    path: 'orders',
    title: 'Your orders · MarketHub',
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER'] },
    loadComponent: () => import('./features/orders/orders.page').then((m) => m.OrdersPage),
  },
  {
    path: 'merchant/products',
    title: 'Your products · MarketHub',
    canActivate: [authGuard],
    data: { roles: ['MERCHANT'] },
    loadComponent: () =>
      import('./features/merchant-dashboard/products.page').then((m) => m.MerchantProductsPage),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    data: { roles: ['ADMIN'] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'categories' },
      {
        path: 'categories',
        title: 'Manage categories · MarketHub',
        loadComponent: () =>
          import('./features/administration/categories.page').then((m) => m.AdminCategoriesPage),
      },
      {
        path: 'merchants',
        title: 'Review shops · MarketHub',
        loadComponent: () =>
          import('./features/administration/merchants.page').then((m) => m.AdminMerchantsPage),
      },
      {
        path: 'customers',
        title: 'Manage customers · MarketHub',
        loadComponent: () =>
          import('./features/administration/customers.page').then((m) => m.AdminCustomersPage),
      },
    ],
  },
  {
    path: 'access-denied',
    title: 'Access restricted · MarketHub',
    loadComponent: () => import('./shared/ui/status.page').then((m) => m.StatusPage),
    data: { denied: true },
  },
  {
    path: '**',
    title: 'Page not found · MarketHub',
    loadComponent: () => import('./shared/ui/status.page').then((m) => m.StatusPage),
  },
];
