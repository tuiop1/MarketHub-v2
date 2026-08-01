# MarketHub Production Deployment

Production uses `docker-compose.prod.yml`.  
The existing `docker-compose.yml` remains for local development.

```text
Internet → Caddy/frontend :80/:443
              ├── /auth/ → Keycloak
              ├── /api/  → gateway-service
              └── /      → Angular files
```

Caddy terminates public HTTPS, serves the Angular application, proxies backend
requests, and automatically obtains and renews the Let's Encrypt certificate.

## Prerequisites

The VPS must have:

- Git
- Docker and Docker Compose
- JDK 25
- Maven

The firewall must allow inbound TCP ports `22`, `80`, and `443`. Allow inbound
UDP port `443` as well to enable HTTP/3; HTTPS still works over TCP without it.

## First deployment

```bash
cd /opt

git clone https://github.com/tuiop1/MarketHub-v2.git
cd MarketHub-v2

cp .env.prod.example .env.prod
nano .env.prod
chmod 600 .env.prod
```

Set the real public server IP, ACME contact email, passwords, and secrets in
`.env.prod`.

When upgrading an existing production installation, rename `CERTBOT_EMAIL` in
`.env.prod` to `ACME_EMAIL`.

Build backend JAR files:

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

If the old local Compose stack is running on the VPS:

```bash
docker compose down
```

Start production:

```bash
docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  config --quiet

docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  up -d --build --remove-orphans
```

`--remove-orphans` stops the removed Nginx and Certbot containers during a
migration, freeing ports `80` and `443`. It does not delete named volumes.

Check the containers:

```bash
docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  ps
```

## Automatic HTTPS

Caddy requests the public IP certificate during startup and serves it as soon
as Let's Encrypt issues it. There is no separate certificate command or proxy
restart. Follow initial issuance with:

```bash
docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  logs -f frontend
```

The server must own the configured public IP, and ports `80` and `443` must be
reachable from the internet for ACME validation.

Open:

```text
https://SERVER_IP
```

Keycloak is available at:

```text
https://SERVER_IP/auth/admin/
```

## Verify the deployment

```bash
SERVER_IP="$(sed -n 's/^SERVER_IP=//p' .env.prod)"
test -n "$SERVER_IP"

curl -fsSI "https://$SERVER_IP"

curl -fsS \
  "https://$SERVER_IP/auth/realms/markethub/.well-known/openid-configuration"
```

View important logs:

```bash
docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  logs --tail=100 frontend keycloak gateway-service
```

## Deploy later updates

```bash
cd /opt/MarketHub-v2
git pull --ff-only

cd backend
mvn clean package -DskipTests
cd ..

docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  up -d --build --remove-orphans
```

## Stop production

```bash
docker compose \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  down
```

Do not add `-v` unless all production database and application volumes should be deleted.

## Important notes

- Never commit `.env.prod`.
- Only ports `22`, `80`, and `443` should be publicly accessible.
- Let's Encrypt IP certificates are short-lived. Caddy renews them automatically;
  the `caddy-data` volume must remain persistent.
- A realm JSON import creates a missing realm but does not overwrite an existing realm.
- If the server IP changes, update `.env.prod` and rebuild/recreate `frontend` so
  Caddy obtains the replacement certificate and Angular receives the new public URL.
