# MarketHub Angular frontend

A routed Angular 20 marketplace with public browsing, customer/cart/order flows, merchant product management and marketplace administration. Source of truth: the Java backend contracts documented in [API_INTEGRATION.md](API_INTEGRATION.md).

## Run locally

Use Node 22.12+ (the Docker build uses Node 22; Node 24 was used for local verification).

```sh
npm install
npm start
```

Open http://localhost:3000. The dev proxy forwards `/api/**` to the API Gateway at http://localhost:8080. Keycloak is http://localhost:8090, realm `markethub`, public client `markethub-client`. The existing backend stack must be running for real data and sign-in. The frontend remains navigable and displays safe errors when the backend is unavailable.

```sh
npm run build
npm test
npm run format:check
```

`npm run build` uses the production environment and emits `dist/markethub/browser`. A direct production build defaults to same-origin `/auth`; the existing Docker build can replace the Keycloak URL through `PUBLIC_KEYCLOAK_URL`. `npm run build -- --configuration=local` uses the local Keycloak origin for the existing Nginx Docker target. Caddy and Nginx already provide client-route fallback. No backend or deployment file was changed.

Tokens stay in memory. After a full page reload, sign in again to reuse the Keycloak SSO session. No client secret, demo identity or application mock-data mode is included. Payments are explicitly simulated by the existing backend.

## API diagnostics

Sign in with an **Admin** account, open **Administration**, then use the **Diagnostics** switch beside the Categories, Shops and Customers tabs. It is available in every build, including production, and starts off. Enable it before navigating or performing an action; the panel shows the latest 25 API requests, including pending/cancelled requests, HTTP status, duration and request/correlation IDs when supplied. The panel appears in the bottom-right corner and stays visible across pages. Collapse it, clear the list, close it, or switch it off from Administration. Recording is memory-only; switching it off clears the list, a full reload resets it, and signing out or losing the Admin role disables it and clears the list.

Only same-origin `/api/v1/` metadata is captured. Request/response bodies, request headers, query values and Keycloak requests are excluded. The panel shows **Caching policy**, based on the configured backend rules: individual product/category GET requests with UUID IDs are **Eligible (up to 30 min)**; lists, administration requests and mutations are **Not configured**. This is a frontend reference to the backend policy, not a live check. It never claims a hit, miss, revalidation or stored response, and does not read cache headers. Keep this small mapping in sync if the backend cache rules or TTL change. No backend change or frontend response cache was added.

Access is controlled by the existing Admin role, not an environment flag. Anonymous users, customers and merchants cannot enable recording or see the panel. Normal user-facing errors and success messages remain with the relevant workflow.

## Keycloak theme

The native Keycloak sign-in screens now share the frontend's plum/stone tokens and self-hosted Plus Jakarta Sans. Keycloak still owns its forms, recovery flow and authentication behavior. See [theme maintenance](../docker/keycloak/themes/markethub/README.md).

```sh
npm run build:keycloak-theme
npm run check:keycloak-theme
```

The SCSS source is `src/keycloak/login.scss`; the build generates the CSS/font assets loaded by the existing Keycloak theme. Rebuild these assets after changing shared tokens, then restart the normal Keycloak service (or redeploy) to clear its theme cache. The Angular application build remains independent.

## Documentation

- [Architecture](ARCHITECTURE.md): ownership, lifecycle, state, auth, routes and trade-offs.
- [API integration](API_INTEGRATION.md): verified endpoint matrix, DTOs, roles and limitations.
- [Learning guide](LEARNING_GUIDE.md): examples for a Java/Spring developer learning Angular.
- [Rebuild notes](REBUILD_NOTES.md): audit, changes, tests and acceptance status.
- [SCSS architecture](src/styles/README.md): external templates, Sass modules, tokens, responsive mixins and cascade rules.
- [Design decisions](DESIGN.md): tokens, typography, accessibility and layout.

Implementation was AI-assisted. Review and live integration testing remain necessary before production release or claims of independently authored implementation.
