#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
# Release bundles carry their own image tag; the shared .env keeps only configuration.
if [[ -z "${RELEASE_TAG:-}" && -f "$ROOT/.release-tag" ]]; then
  export RELEASE_TAG
  RELEASE_TAG=$(cat "$ROOT/.release-tag")
fi
exec docker compose \
  --project-directory "$ROOT" \
  --env-file "${AWS_ENV_FILE:-$ROOT/production/aws/.env}" \
  -f "$ROOT/production/aws/docker-compose.yml" "$@"
