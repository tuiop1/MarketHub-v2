# Frontend architecture

Angular 20 standalone application. The rebuild replaces manual view switching and the central API/component with routed business features. It intentionally uses Signals, HttpClient, typed Reactive Forms and RxJS, without a global state framework or UI component framework.

## Structure

```text
src/
  main.ts                         application bootstrap and startup fallback
  environments/                  local and production identity-provider configuration
  styles.scss                    imports the SCSS composition root
  styles/                        tokens, reset, base, layout, shared patterns, utilities
  app/
    app.component.{ts,html,scss}   shell, navigation, footer, session-end navigation
    app.config.ts                Router, HTTP interceptors, auth initialization
    app.routes.ts                lazy routes, page titles, role guards and 404
    core/
      auth/                      official Keycloak adapter, guards, token interceptor
      http/                      safe API error normalization
    features/
      authentication/            sign-in and callback pages
      registration/              customer and merchant typed registration
      catalog/                   API/models, product card, public product/category pages
      merchants/                 public shop directory and detail pages
      account/                   identity/profile DTOs, API, customer/shop status page
      cart/                      API, cart page, cart line presentation
      orders/                    API/models, checkout, history and snapshot details
      merchant-dashboard/        own products, product form and paginated category picker
      administration/            category form/list, shop review and customer access
    shared/
      models/                    Spring Page shape and pagination parameters
      state/                     cancelable read resources and local mutation state
      ui/                        icons, feedback, loading/empty states, fields, money,
                                 pagination, confirmation, 404
      utilities/                 nonblank, integer and past-date validators
    testing/                     test-only fixtures and cross-feature API contract tests
```

Tests are colocated with the behavior they cover. Every Angular component uses `templateUrl` to reference a separate HTML file, including small shared components and routing-test fixtures. Component/page styles are colocated `.scss` files referenced by `styleUrl`. `styles/main.scss` composes the global Sass partials inside explicit native cascade layers; runtime design tokens remain CSS custom properties. The CSS-free `styles/tools/_media.scss` module shares existing breakpoint values and mixins. No inline templates, decorator styles or style attributes remain. See [SCSS architecture](src/styles/README.md) for ownership, module use and cascade rules. `DESIGN.md` records the visual decisions.

## Responsibility boundaries

A page coordinates a user workflow. `ProductListPage` owns routed sort/page state and the selection for batch checkout. `ProductCardComponent` presents one product and owns its quantity/add action. `CheckoutComponent` owns payment selection, a purchase request and its result. `CartPage` coordinates cart reads and line mutations. `CartItemComponent` emits increase/remove intentions; it never calls HTTP.

Management forms own validation and create/update requests. Their parent pages own list pagination and selection of the editing record. Form `busyChange` outputs prevent the parent switching records during a save. `ConfirmationComponent` owns native modal presentation, not the administrative action.

API clients contain HTTP methods and DTOs, with no navigation, notifications or component state. Catalog, account, merchants, cart, orders, merchant products and administration have distinct clients. Account models are also used by the shop directory and administration: these are the same backend DTO, not duplicated frontend variations.

## Reads and state

`requestState()` converts an HTTP stream into loading/success/error state. Empty is derived from successful content, rather than stored as another mutable flag. `pagedResource()` reads whitelisted sort values, a nonnegative zero-based page and an optional admin view from Router query parameters. `detailResource()` reads the route ID. Both use `switchMap`: a newer route/query/retry cancels the older read. `toSignal` connects the stream to the template and destroys its subscription with the component.

Synchronous UI state uses `signal` and `computed`: selected products, confirmation targets, editor selection, availability and derived totals. There is no global cart cache to reconcile across identities. Cart reads use current backend prices and stock; order history uses immutable backend snapshots.

`ActionState` is a small composed helper for mutations. It prevents duplicate submissions, exposes a local error/message, cancels the HTTP subscription when the owner is destroyed and never retries a mutation. A failed save keeps form values. A failed checkout keeps the cart. Uncertain purchase failures disable another submission on that checkout instance and link to order history. This is a user-experience safeguard; only a backend idempotency contract could guarantee exactly-once purchase across reloads/tabs.

## Authentication and routing

`AuthService` initializes `keycloak-js` before Router initial navigation through `provideAppInitializer`. The public client is `markethub-client` in realm `markethub`. Authorization code flow uses PKCE S256, state and nonce checks supplied by the adapter. `/callback` is the already permitted callback. The application saves only the intended internal return route in session storage; adapter tokens remain in memory. Legacy client token keys are removed.

Anonymous browsing initializes without `onLoad: check-sso`, so visiting the catalog does not redirect through Keycloak or depend on its availability. After a full page reload the in-memory session is gone: the user signs in again through the existing Keycloak SSO session. This is a deliberate simplicity/availability trade-off. Automatic SSO restoration would need a separately considered redirect/iframe strategy compatible with the exact callback allowlist and browser cookie restrictions; it is not silently enabled.

Before an authenticated gateway request, the adapter renews a near-expiry access token using its actual refresh token. `onTokenExpired` also attempts renewal. Refresh failure or API 401 clears the session and returns the user to sign-in. No purchase is automatically replayed after a 401. Logout uses the adapter end-session URL, callback redirect and ID-token hint when available.

`authGuard` redirects anonymous protected navigation to `/login?returnTo=...`. Role requirements redirect a mismatched identity to `/access-denied`. Admin child routes re-run the guard. Guards and navigation visibility improve UX; the gateway and downstream Spring security remain authoritative. Product mutation services also validate merchant ownership and VERIFIED status.

## HTTP and errors

All application clients use relative `/api/v1` paths. The dev proxy forwards these to :8080; the existing Nginx/Caddy deployments keep their gateway routing. The browser never calls payment, reservation, discovery, database, Redis, Kafka or notification endpoints.

Interceptors execute in registration order: auth attaches a renewed token to eligible same-origin gateway requests; error handling translates downstream failures; auth observes normalized 401s on the return path. Registration and foreign URLs never receive access tokens. Raw backend exceptions, stack traces, request bodies and credentials are never rendered or logged by the application.

`ApiFailure` carries a status, known business code, safe message and validation field paths. Form fields display validation near the control. Unknown server messages are replaced with safe status-specific text. A specific `Cart was not found with customerId:` response is empty; other 404s are not discarded because cart enrichment may fail on unavailable items.

## Forms and accessibility

Typed Reactive Forms preserve DTO boundaries. Required, nonblank, email, length, integer, price, stock, past-date and category rules are checked before HTTP. Customer-only controls are disabled in merchant mode and vice versa. Passwords and addresses start empty. Successful registration clears the password. Product prices use explicit USD cents rather than lossy floating-point conversions.

Submit controls belong to a `FormGroup` directive, including small one-control quantity/payment forms. This is necessary for `ngSubmit`; a standalone `formControl` alone does not install a form submit handler.

Semantic labels, descriptions and inline live errors support form navigation. The shell has a working focus-moving skip link, keyboard focus styles and an accessible mobile toggle. Native `dialog.showModal()` provides modal focus containment and Escape handling; Cancel receives initial focus. Native order `details` supports keyboard expansion. Responsive tables scroll within their container. Formal WCAG certification and real screen-reader testing have not been performed.

## Testing and trade-offs

Vitest and Angular TestBed test real components and the real Keycloak adapter with controlled HTTP/fetch responses. Contract tests verify exact gateway requests. Router tests verify guards and cancellation. Tests cover forms, confirmations, checkout outcomes and shell visibility. Browser checks separately verify public navigation, responsive layout and registration validation.

There is no speculative state library, generic CRUD framework, backend alteration, image API, real payment UI or client-side filtering pretending to search the entire paginated catalog. `API_INTEGRATION.md` lists the exact contracts and unsupported capabilities. `REBUILD_NOTES.md` records verification limits and follow-up work.

Reference guidance: [official Keycloak JavaScript adapter](https://www.keycloak.org/securing-apps/javascript-adapter), [Angular route guards](https://angular.dev/guide/routing/route-guards), [Angular Reactive Forms](https://angular.dev/guide/forms/reactive-forms). The local Angular 20 types, Java contracts and automated tests determine the implementation details in this repository.
