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

## Documentation

- [Architecture](ARCHITECTURE.md): ownership, lifecycle, state, auth, routes and trade-offs.
- [API integration](API_INTEGRATION.md): verified endpoint matrix, DTOs, roles and limitations.
- [Learning guide](LEARNING_GUIDE.md): examples for a Java/Spring developer learning Angular.
- [Rebuild notes](REBUILD_NOTES.md): audit, changes, tests and acceptance status.
- [CSS architecture](src/styles/README.md): stylesheet ownership, tokens, units and cascade rules.
- [Design decisions](DESIGN.md): tokens, typography, accessibility and layout.

Implementation was AI-assisted. Review and live integration testing remain necessary before production release or claims of independently authored implementation.
