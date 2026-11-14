#!/usr/bin/env bash
set -euo pipefail
for variable in PGHOST PGUSER PGPASSWORD ACCOUNT_DB_PASSWORD CATALOG_DB_PASSWORD CART_DB_PASSWORD ORDER_DB_PASSWORD PAYMENT_DB_PASSWORD KEYCLOAK_DB_PASSWORD; do
  if [[ -z "${!variable:-}" || "${!variable}" == CHANGE_ME* ]]; then
    printf 'Set a real value for %s before initializing RDS.\n' "$variable" >&2
    exit 1
  fi
done
exec psql --no-psqlrc --set=ON_ERROR_STOP=1 --file=/init/init.sql
