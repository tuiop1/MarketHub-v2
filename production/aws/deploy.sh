#!/usr/bin/env bash
# Runs on EC2 as ubuntu. Never build images or remove persistent volumes here.
set -Eeuo pipefail
umask 077

ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
STATE=${MARKETHUB_DEPLOY_ROOT:-/opt/markethub}
SHARED=${AWS_ENV_FILE:-/opt/MarketHub-v2/production/aws/.env}
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${PUBLIC_APP_URL:?PUBLIC_APP_URL is required}"
[[ "$RELEASE_TAG" =~ ^[a-f0-9]{40}$ ]] || { echo 'Invalid release SHA' >&2; exit 1; }
[[ "$ROOT" == "$STATE/releases/$RELEASE_TAG" ]] || {
  echo 'Run deployment only from its prepared release directory.' >&2; exit 1;
}
[[ "$PUBLIC_APP_URL" =~ ^https://[a-zA-Z0-9.-]+$ ]] || exit 1
[[ -r "$SHARED" && -r "$(dirname "$SHARED")/certs/global-bundle.pem" ]]
mkdir -p "$STATE"
exec 9>"$STATE/deploy.lock"
flock -w 30 9

PREVIOUS=$(readlink -f "$STATE/current")
[[ -d "$PREVIOUS" && -r "$PREVIOUS/.release-tag" ]] || {
  echo 'Bootstrap the current release before deploying.' >&2; exit 1;
}
PREVIOUS_TAG=$(cat "$PREVIOUS/.release-tag")
[[ "$PREVIOUS_TAG" =~ ^[a-f0-9]{40}$ ]] || exit 1
printf '%s\n' "$RELEASE_TAG" > "$ROOT/.release-tag"
ln -sfn "$SHARED" "$ROOT/production/aws/.env"
mkdir -p "$ROOT/production/aws/certs"
ln -sfn "$(dirname "$SHARED")/certs/global-bundle.pem" "$ROOT/production/aws/certs/global-bundle.pem"
export AWS_ENV_FILE="$SHARED"
compose() { bash "$ROOT/production/aws/compose.sh" --profile observability "$@"; }

compose config --quiet
IMAGE=$(compose config --images gateway-service)
REGISTRY=${IMAGE%%/*}
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
[[ "$REGISTRY" == "$ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com" ]] || {
  echo 'ECR registry does not match the EC2 AWS account and region.' >&2; exit 1;
}
aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$REGISTRY"
timeout 600 bash "$ROOT/production/aws/compose.sh" --profile observability --parallel 2 pull

rollback() {
  local result=$?
  trap - ERR
  echo "Deployment failed; restoring $PREVIOUS_TAG" >&2
  if RELEASE_TAG="$PREVIOUS_TAG" bash "$PREVIOUS/production/aws/compose.sh" \
      --profile observability up -d --wait --wait-timeout 600; then
    echo 'Previous containers restored. Database migrations are not reversed.' >&2
  else
    echo 'ROLLBACK FAILED: inspect EC2 before retrying.' >&2
  fi
  exit "$result"
}
trap rollback ERR
compose up -d --wait --wait-timeout 600

# Catch restart loops in tools without Docker HEALTHCHECK, not just initial startup.
SERVICE_LINES=$(compose config --services)
mapfile -t SERVICES <<< "$SERVICE_LINES"
EXPECTED_COUNT=${#SERVICES[@]}
export EXPECTED_COUNT
ID_LINES=$(compose ps -a -q "${SERVICES[@]}")
mapfile -t IDS <<< "$ID_LINES"
# Only compare restarts during this rollout; an old restart is not a new failure.
BASELINE=$(docker inspect --format '{{.Id}} {{.RestartCount}}' "${IDS[@]}")
export BASELINE
for _ in {1..6}; do
  sleep 10
  ID_LINES=$(compose ps -a -q "${SERVICES[@]}")
  mapfile -t IDS <<< "$ID_LINES"
  docker inspect "${IDS[@]}" | python3 -c '
import json, os, sys
containers = json.load(sys.stdin)
baseline = dict(line.split() for line in os.environ["BASELINE"].splitlines())
bad = [c["Name"] for c in containers if c["State"]["Status"] != "running"
       or c["State"].get("Health", {}).get("Status", "healthy") != "healthy"
       or str(c["RestartCount"]) != baseline.get(c["Id"])]
if len(containers) != int(os.environ["EXPECTED_COUNT"]) or bad:
    sys.exit("Unstable containers: " + str(bad))
'
done
READY=false
for _ in {1..30}; do
  if docker exec markethub-v2-aws-caddy-1 wget -qO- http://tempo:3200/ready > /dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 5
done
"$READY"
for path in '/api/v1/products?size=1' '/auth/realms/markethub/.well-known/openid-configuration'; do
  curl --fail --silent --show-error --retry 5 --retry-all-errors --retry-delay 5 \
    --max-time 20 "$PUBLIC_APP_URL$path" | python3 -c 'import json, sys; json.load(sys.stdin)'
done

# Publish the pointer only after health checks pass; no git reset and no .env rewrite.
if [[ "$PREVIOUS" != "$ROOT" ]]; then
  ln -sfn "$PREVIOUS" "$STATE/previous.next"
  mv -Tf "$STATE/previous.next" "$STATE/previous"
fi
ln -sfn "$ROOT" "$STATE/current.next"
mv -Tf "$STATE/current.next" "$STATE/current"
trap - ERR
echo "Backend release $RELEASE_TAG is healthy."
