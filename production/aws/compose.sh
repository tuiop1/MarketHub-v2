#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
exec docker compose \
  --project-directory "$ROOT" \
  --env-file "${AWS_ENV_FILE:-$ROOT/production/aws/.env}" \
  -f "$ROOT/production/aws/docker-compose.yml" "$@"
