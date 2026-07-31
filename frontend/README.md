# MarketHub frontend

Small Angular UI for exercising the flow in `http/markethub-full-flow.http`.

## Run

Start the backend and Keycloak first from the repository root:

```bash
docker compose up -d
```

Then start the UI:

```bash
cd frontend
npm install
npm start
```

Open <http://localhost:3000>. The development proxy forwards `/api` requests to
the gateway at port 8080. Login uses Keycloak at port 8090 with Authorization
Code + PKCE.

The frontend is also part of the root Docker Compose stack:

```bash
docker compose up -d --build frontend
```

Its Nginx container serves the Angular application on port 3000 and proxies
`/api` to `gateway-service` over the Compose network.

The Keycloak client `markethub-client` must allow:

- Valid redirect URI: `http://localhost:3000/callback`
- Web origin: `http://localhost:3000`

After an admin verifies a pending merchant, that merchant must log out and log
in again so the new token contains the `MERCHANT` realm role.
