# Rebuild notes

## Audit before implementation

| Existing problem                                              | Why it matters                                                    | Change and benefit                                                           |
| ------------------------------------------------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| ~624-line root class and ~598-line template own all domains   | Coupled lifecycles and unrelated failures                         | Feature pages own their requests, forms and actions; root owns layout only   |
| View signal replaces routing                                  | No deep links/history/refresh navigation                          | Angular Router with lazy pages, guards, callback and 404                     |
| One API service, unknown registration bodies, string statuses | Contract drift is easy                                            | Focused APIs and exact DTO/enums                                             |
| Session-storage tokens and custom OAuth implementation        | Credentials persist in JS storage; missing mature renewal support | Official Keycloak adapter, code/PKCE, in-memory tokens                       |
| Global busy/result state and raw error JSON                   | Unrelated actions block one another; sensitive diagnostics leak   | Local request/action state, safe translated errors and inline validation     |
| Concurrent promise-based list loads                           | Older responses may overwrite new sort/page                       | switchMap cancels outdated reads                                             |
| Demo personal details and random form presets                 | Registration can submit unrelated personal information            | Empty typed reactive forms                                                   |
| Quantity clamping silently changes invalid input              | Zero stock and invalid numbers are confusing                      | Visible validation; reject invalid quantities                                |
| Few component tests; customer administration absent           | Regressions and backend capabilities missed                       | HTTP, routing, auth, form, UI and checkout tests; customer enable/disable UI |
| Broad global CSS and low-contrast small labels                | Styling collisions and accessibility issues                       | Documented tokens, scoped component styles and shared semantic patterns      |

## Implementation plan

1. Verify contracts and preserve original frontend (completed before Angular edits).
2. Establish standalone Router, auth adapter, error boundary/interceptors, typed APIs and shared UI.
3. Build public catalog/category/merchant routes; then registration/account, cart/orders/direct checkout, merchant management and administration.
4. Verify strict compilation, production build, meaningful Vitest coverage, browser desktop/mobile and container configuration where possible.
5. Document architecture, actual contracts, learning examples and exact verification limitations.

Work branch: codex/angular-frontend-rebuild. Original tracked frontend: codex/frontend-before-rebuild at bb5a7fa. Existing backend and realm edits are preserved. AI assisted implementation; developer review and live integration verification remain necessary before presenting it as independently authored work.

## Final implementation

The root now contains only shell/navigation behavior; feature pages and focused clients own the marketplace workflows. Lazy Angular routes replace the custom view signal. Typed registration, product and category forms replace demo presets. Local mutation state replaces the global busy/result switch. A failed request no longer disables unrelated areas. The official Keycloak adapter replaces custom token parsing and OAuth exchange code.

Public products/categories/shops have paginated directory and detail routes. Customer features include registration, account details, cart additions/removal/clear, direct single/batch purchase, cart checkout and expandable order snapshots. Approved merchants can create, edit, activate and deactivate their own products. Administrators can manage categories, approve/reject/suspend shops and enable/disable customer accounts. Sensitive management operations require confirmation.

The visual design uses plum, stone and an ochre brand accent after the user rejected blue/white. Product placeholders are explicitly image-free; no image contract was invented. Plus Jakarta Sans is bundled locally. On narrow mobile screens the secondary shop promotion is hidden so product browsing starts sooner; its actions remain in navigation/footer.

## Verification on 2026-11-11

| Check                                   | Result                                                                                                                                                                                                                                                                                                      |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm install`                           | Passed with matching Angular runtime/compiler 20.3.35 and build/CLI 20.3.42. An initial npm peer-resolution failure was fixed by a clean compatible dependency resolution; no `--force` or ignored peers.                                                                                                   |
| `npx tsc --noEmit -p tsconfig.app.json` | Passed, including strict templates through the Angular build and no-unused checks.                                                                                                                                                                                                                          |
| `npm test`                              | 81 tests passed across 10 files. Includes the real Keycloak adapter with controlled fetch, API contracts, roles, forms, request cancellation, confirmations and checkout outcomes.                                                                                                                          |
| `npm run build`                         | Passed. Production assets at `dist/markethub/browser`; initial JS/CSS total 361.61 kB raw, CLI estimated transfer 101.67 kB. These are build output figures, not measured browser performance.                                                                                                              |
| `npm run format:check`                  | Final formatting checked with Prettier.                                                                                                                                                                                                                                                                     |
| `git diff --check`                      | Passed.                                                                                                                                                                                                                                                                                                     |
| Production bundle origin scan           | No `localhost:8080` or `localhost:8090` in production JavaScript.                                                                                                                                                                                                                                           |
| `npm audit --omit=dev`                  | 0 production dependency vulnerabilities at verification time.                                                                                                                                                                                                                                               |
| Full `npm audit`                        | 20 development dependency findings: 18 moderate and 2 critical. The critical findings are Vitest 3/tinypool; Angular 20's test builder declares Vitest `^3.1.1`. Do not force incompatible major peers. A supported test-toolchain upgrade is follow-up work before relying on exposed development tooling. |
| Browser inspection                      | Public routes, populated catalog, sorting, pagination, direct product refresh, category navigation, registration validation/mode switch, mobile menu and skip-link focus checked. No horizontal overflow at actual CSS widths 1440, 960, 390 and 409 in the pages inspected.                                |
| Browser console                         | No error/warning entries on the final product/detail and registration checks. API connection errors are expected on the real app while its backend is stopped.                                                                                                                                              |
| Design detector                         | One advisory about the popularity of Plus Jakarta Sans. Retained intentionally for legible UI; no mechanical design violations reported.                                                                                                                                                                    |
| Backend/infrastructure integrity        | SHA-256 comparison against the start-of-task tracked backend/Docker/Compose files showed no changes introduced by this rebuild. Existing user changes remain untouched.                                                                                                                                     |

The populated browser check used a separate read-only temporary HTTP fixture server on :8088 and Angular preview on :3002. This was visual testing, not real integration. The application has no mock-data mode. `qa/catalog-mobile-fixtures.jpg` is a screenshot of that isolated test. The actual app preview uses :3000 with the real gateway proxy.

Tests found and fixed a real missing-FormGroup submit-handler issue in small forms. A rendered-button checkout test now protects that behavior. Other fixes include retaining the paid receipt after cart reload, guarding repeated submissions, cancelling stale list reads, refusing ambiguous cart 404s, removing credentials from callback URLs and preventing output emission after form destruction.

## Acceptance status and limitations

| Criterion                                                                 | Implementation / verification status                                                                                                                              |
| ------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend analysis and exact contracts                                      | Complete; API matrix checked against local source.                                                                                                                |
| Feature architecture, focused components/clients, Router and strict types | Implemented and compiled.                                                                                                                                         |
| Keycloak login/state/nonce/PKCE/refresh/logout                            | Implemented; adapter tests pass. Live identity-provider/browser round trip unverified.                                                                            |
| Role-aware navigation and routes                                          | Implemented and unit tested; live gateway authorization unverified.                                                                                               |
| Public browsing, category/shop details and pagination                     | Implemented; browser tested with isolated read-only HTTP fixtures. Real catalog unverified.                                                                       |
| Customer registration/profile/cart/orders/direct purchase                 | Implemented; HTTP/component tests pass. Real multi-service transactions unverified.                                                                               |
| Merchant registration/status/product management                           | Implemented; form and contract tests pass. Real approval and token-role transition unverified.                                                                    |
| Administrator workflows                                                   | Implemented; contract and confirmation tests pass. Real account/Keycloak mutation unverified.                                                                     |
| Loading/error/empty states and duplicate prevention                       | Implemented and tested.                                                                                                                                           |
| Responsive/accessibility                                                  | Public desktop/mobile inspection and keyboard/validation checks completed. Authenticated-screen visual QA and a real screen-reader/WCAG audit remain unverified.  |
| Production build                                                          | Passed.                                                                                                                                                           |
| Docker/production routing                                                 | Existing Dockerfile/Nginx/Caddy/Compose files preserved; image build, TLS/proxy routing and live callback through Caddy unverified.                               |
| Documentation                                                             | Architecture, API integration, learning guide, audit, design and run instructions written.                                                                        |
| No critical functional regression                                         | No known functional regression in the checks run; live workflows cannot be certified without the stack. Development-tool dependency findings remain listed above. |

Docker was unavailable: the configured socket `/home/tuiop/.docker/desktop/docker.sock` did not exist. Gateway :8080 and Keycloak :8090 refused connections. No real accounts, payments, approval decisions or backend data were created during verification. To close the live acceptance items, start the existing Docker stack and exercise customer registration/login/cart/direct and cart purchase (including QR failure), merchant registration/approval/fresh-token/product CRUD, administrator transitions and production callback/deep-route refresh.

Deliberate limitations and trade-offs:

- Tokens remain in memory, so a full browser reload requires explicit sign-in again using Keycloak SSO. This preserves anonymous browsing without an identity-provider redirect.
- UI duplicate prevention cannot guarantee exactly-once ordering across reloads/tabs. The backend has no browser idempotency-key contract. Do not automatically retry ambiguous purchases.
- Unavailable cart lines may make the entire backend cart read fail. The frontend offers refresh or a confirmed clear; it cannot reconstruct hidden line IDs.
- Product lists can contain a listing whose merchant no longer qualifies to sell, while details/purchase reject it. This is backend behavior, documented without alteration.
- There is no backend search/category-product/merchant-product public filter, product-image API, account-edit API, merchant re-enable/all-status list, order cancellation or single-order lookup. These are unsupported, not silently mocked.
- Currency is not supplied by the API. The original frontend's USD display convention is retained and documented.
- No broad global store was introduced. Re-entering a feature reads fresh backend data; this trades extra reads for simpler identity isolation and fewer stale caches.

Implementation and documentation were AI-assisted. Review, extend and understand the code before presenting it in an interview; do not claim independent authorship or unperformed end-to-end validation.

## CSS architecture follow-up

The CSS was subsequently reorganized without changing API clients, authentication or business workflows. `src/styles.css` now imports a single `styles/main.css` composition root. Design tokens, reset, element defaults, reusable layouts, shared UI patterns, utilities and accessibility overrides have separate files and explicit cascade layers. Component-owned CSS was extracted from decorators into colocated stylesheets; static inline style attributes were replaced with local classes. Loading, empty-state, pagination and catalog-grid styles now belong to their components/pages. Shared table selectors are constrained to `.table-wrap`.

Typography, colors, spacing, radii, control dimensions, common widths, motion and elevation use shared custom properties. Rem values preserve the original default reference dimensions while allowing browser font preferences to scale them. Utility classes have the explicit `u-` prefix. No dependency or backend/infrastructure change was needed. The extension rules are documented in `src/styles/README.md`; Angular still uses its existing `src/styles.css` entry.

Verification for this follow-up: production build passed; all 81 tests across 10 files passed; formatting and Git whitespace checks passed. Browser comparison at CSS width 1280 found no differences across 132 sampled computed-style values for registration, typography, controls, shell and layout. Mobile checks at CSS width 390 confirmed a single-column form, visible inline validation, working sort-control layout and no horizontal overflow. `qa/css-refactor-mobile.jpg` records the post-refactor registration screen using no fixture data. These checks cover the refactor; they do not turn the earlier unverified backend workflows into verified ones.
