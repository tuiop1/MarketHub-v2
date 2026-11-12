# Learning this Angular implementation

Read this alongside the actual files. Your Spring experience transfers well: dependency injection, HTTP contracts and separating controllers from services are useful analogies. Angular also has component lifecycles and asynchronous rendering that need their own mental model.

## 1. Start at bootstrap and the root

`src/main.ts` calls `bootstrapApplication(AppComponent, appConfig)`. A standalone component declares its template dependencies directly in `imports`; there is no application NgModule. `app.config.ts` registers application-wide providers. `app.component.ts` owns the shell and navigation, while the routed feature owns the business workflow.

A component is a class plus a template and optionally scoped styles. It is closer to a small presentation controller than a Spring service. Avoid putting every workflow in the root just because it can access every service.

## 2. Dependency injection

Open `features/catalog/data-access/catalog-api.service.ts`. `inject(HttpClient)` asks Angular's injector for a dependency, much like constructor injection in Spring. `providedIn: 'root'` supplies a shared service instance without creating a custom module.

Most API services can be application singletons because they hold no user-interface state. `ActionState`, in contrast, is composed locally by a component with its own `DestroyRef`. Two forms therefore do not share loading or error flags.

`AUTH_CLIENT` is an injection token with an official Keycloak adapter factory. Tests replace this dependency or provide a real adapter with a controlled token response; production code does not need a testing mode.

## 3. Signals and derived state

In `cart/cart.page.ts`, `checkoutBusy` is a writable signal and `canCheckout` is computed from current cart data. Call a signal to read it, and use `.set()` or `.update()` to change writable state. Angular tracks signal reads in templates.

A computed signal is like a cached derived getter with explicit reactive dependencies. Do not separately store totals or availability if they can be derived from existing state. `ProductListPage.selectedTotal` derives its value from the visible products and the validated selection.

Effects are reserved for real side effects: updating conditional form controls when registration mode changes, resetting an editor from its selected record, or reacting to session termination. They are not a replacement for ordinary computed values.

## 4. Observables and cancellation

Open `shared/state/paged-resource.ts` and follow its pipeline:

1. Router query parameters emit a page/sort/view.
2. `distinctUntilChanged` ignores equivalent query values.
3. `combineLatest` combines that query with a retry trigger.
4. `switchMap` starts the new HTTP read and unsubscribes the old one.
5. `requestState` maps loading, successful data and safe errors.
6. `toSignal` exposes the latest state to the template and handles teardown.

An Observable describes a stream; an HttpClient Observable starts its request when subscribed. `switchMap` is useful for reads where only the latest result matters. It is not used to repeatedly restart purchases: cancelling the browser request does not roll back work already received by the server.

`firstValueFrom` in `ActionState` lets an event handler await one mutation response. The helper guards duplicate clicks and uses `takeUntilDestroyed`. There are no nested subscriptions to reason about.

## 5. HttpClient and exact DTOs

Compare `orders/data-access/orders-api.service.ts` with the Java `OrderController`. Direct purchase sends an object containing `items` and `paymentMethod`. Cart purchase sends `JSON.stringify(paymentMethod)` with JSON content type. These contracts look similar in the UI but are different on the wire.

TypeScript interfaces describe the expected server DTOs; they do not validate an arbitrary response at runtime. The authoritative contracts are the Java source and `API_INTEGRATION.md`. Use readonly response properties to discourage accidental mutation. Request models deliberately omit server-owned IDs, timestamps and, for product creation, the activation field.

## 6. Router and lazy loading

`app.routes.ts` is the route map. `loadComponent` creates feature chunks so the administration code need not be loaded for a public catalog visit. `RouterLink` creates real navigable links. Browser history, direct links and refresh work with the server's existing index.html fallback.

Page and sort live in query parameters because they belong in a shareable address. A selected editor record or an open confirmation is local component state because it does not need a permanent URL.

`RouterTestingHarness` in `catalog.spec.ts` demonstrates direct query navigation and proves that an older request is cancelled. Use it when behavior depends on the Router rather than constructing a fake URL parser.

## 7. Guards and interceptors

`authGuard` returns true or a Router URL tree. Returning a redirect is clearer than initiating navigation and then returning false. ADMIN, CUSTOMER and merchant roles affect navigation, but Spring still authorizes every actual operation.

Interceptors are request/response middleware, comparable to a client-side filter chain. `auth.interceptor.ts` attaches credentials only to eligible `/api/v1/` requests. `api-error.interceptor.ts` normalizes HTTP errors. Their ordering matters because the auth interceptor reads the normalized 401 on the way back.

Study `auth.service.spec.ts`: it creates a real Keycloak authorization URL, returns a matching state/code, and controls only the token endpoint response. Separate tests reject unmatched state and nonce. This gives stronger evidence than mocking the entire login result, but still does not replace a real Keycloak/browser integration test.

## 8. Component communication

`CartItemComponent` receives `input.required<CartItem>()` and emits `increase`/`remove` outputs. The parent owns the HTTP mutation. This keeps line rendering independently understandable.

`ProductFormComponent` owns a complete form workflow and emits `saved`, `cancelled` and `busyChange`. The parent reloads the list on save and locks record switching while a save is pending. Not every small control needs a service or store; use inputs/outputs when a parent-child relationship is sufficient.

## 9. Typed Reactive Forms

Open `registration/register.page.ts` and its HTML. `FormBuilder.nonNullable` makes ordinary text controls return strings instead of `string | null`. The nested customer/merchant groups preserve structure. The inactive group is disabled so hidden required controls do not prevent submission.

`getRawValue` includes disabled groups, so the submit method explicitly constructs only the DTO for the chosen account type. This resembles mapping a Java command object into an API request instead of serializing a persistence entity.

Client validation improves feedback; server validation is still required. The server may detect duplicates or changed stock after the form passed local checks. `FieldErrorComponent` handles control errors and safe server field paths. This component uses normal change detection because Angular form control objects are mutable; an unchanged object reference alone is insufficient for an OnPush input to signal every validation change.

Look at the small checkout form too: a `formControl` inside a plain form is not enough for `ngSubmit`. The explicit `FormGroup` installs the submit directive. A test clicks the actual checkout button to catch this integration mistake.

## 10. Testing and what to study next

Run `npm test`. `HttpTestingController` captures outbound Angular requests and flushes controlled responses. Test fixtures are confined to test files. It is useful to test payment failure, a stale list request, an uncertain purchase or a missing-cart ambiguity because they reveal workflow errors that a screenshot cannot.

Recommended reading order:

1. `app.component.ts`, `app.config.ts`, `app.routes.ts` — application structure.
2. `catalog-api.service.ts` and `product-list.page.ts` — a complete read flow.
3. `request-state.ts` and `paged-resource.ts` — state, streams and teardown.
4. `product-card.component.ts` and `cart-item.component.ts` — two justified communication styles.
5. `register.page.ts` and `product-form.component.ts` — typed forms and DTO construction.
6. `auth.service.ts`, guards and interceptor tests — session and authorization boundaries.
7. `checkout.component.ts` and `checkout.spec.ts` — duplicate prevention and honest outcomes.
8. `API_INTEGRATION.md` and `REBUILD_NOTES.md` — backend behavior and verification limits.

A good next exercise is adding a test for a changed backend contract before extending a workflow. Do not add frontend search or a product-image field until a real backend contract exists for it. Review and modify the AI-assisted code yourself before explaining it in an interview; describe that assistance accurately.

## CSS ownership

`src/styles.css` imports `src/styles/main.css`, the single composition root. Start with `tokens.css` for design values, `reset.css` for browser normalization and `base.css` for element defaults. Shared visual patterns live under `styles/components`; feature-specific rules live beside their Angular component and load through `styleUrl`. Utilities have the `u-` prefix. Angular emulated encapsulation keeps component selectors local; native cascade layers establish precedence for the shared defaults. See [CSS architecture](src/styles/README.md) for examples and the reason component styles remain unlayered.
