#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
exec docker compose \
  --project-directory "$ROOT" \
  --env-file "${EC2_ENV_FILE:-$ROOT/.env.prod}" \
  -f "$ROOT/production/ec2/docker-compose.prod.yml" "$@"
