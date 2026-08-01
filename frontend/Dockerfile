FROM node:22-alpine AS build

WORKDIR /app

ARG BUILD_CONFIGURATION=local
ARG PUBLIC_KEYCLOAK_URL=http://localhost:8090

COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json tsconfig.json tsconfig.app.json ./
COPY src ./src

RUN if [ "$BUILD_CONFIGURATION" = "production" ]; then \
      sed -i \
        "s|__PUBLIC_KEYCLOAK_URL__|${PUBLIC_KEYCLOAK_URL}|g" \
        src/environments/environment.production.ts; \
    fi \
    && npm run build -- --configuration="${BUILD_CONFIGURATION}"

FROM caddy:2.11.4-alpine AS production

COPY Caddyfile.prod /etc/caddy/Caddyfile
COPY --from=build /app/dist/markethub/browser /srv

EXPOSE 80 443

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:2019/config/ || exit 1

FROM nginx:1.27-alpine AS local

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/markethub/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
