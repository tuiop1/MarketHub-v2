# MarketHub login theme

This is a presentation-only extension of the pinned Keycloak 26.6.4 `keycloak.v2` login theme. It inherits native templates and scripts, including password visibility, errors and password recovery. `theme.properties` loads the parent stylesheet before the MarketHub overrides and selects the light palette used by the application. Realm, client, authentication and backend settings are unchanged.

## Editing and building

Edit `frontend/src/keycloak/login.scss`, not the generated `login/resources/css/login.css`. The SCSS uses the same `frontend/src/styles/_tokens.scss` as Angular. The generated assets include the locally bundled Latin font and its license; there is no third-party font request.

From `frontend/`:

```sh
npm ci
npm run build:keycloak-theme
npm run check:keycloak-theme
```

Commit the generated CSS and font assets with their sources. Keycloak serves CSS directly and does not compile Sass. The standalone build/check script uses the declared Sass development dependency and checks font/license freshness too. It does not change the Angular build or Docker configuration.

The existing Compose mount and deployment packaging already include this theme directory. After updating a running installation, restart Keycloak to invalidate its theme cache. Do not change live realm/client configuration to apply this visual update.

## Verification

On 2026-11-14, the theme was loaded by an isolated Keycloak 26.6.4 test instance. Its parent/custom stylesheets and local font were fetched successfully. Native invalid-login feedback, the authorization-code/PKCE flow and the password-recovery page were checked through HTTP integration tests with a disposable synthetic identity. This is not a test of the real deployment or a rendered browser/layout check: browser automation was unavailable in that session.

Reference: [Keycloak theme customization](https://www.keycloak.org/ui-customization/themes).
