#!/usr/bin/env python3
"""Submit one checksum-verified release to EC2 and wait for its actual result.

Uses the AWS CLI and its OIDC credentials; no Python packages or SSH key required.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time


def aws(*args):
    return json.loads(subprocess.check_output(
        ["aws", *args, "--output", "json"], text=True))


def main():
    release = os.environ["GITHUB_SHA"]
    instance = os.environ["EC2_INSTANCE_ID"]
    bucket = os.environ["FRONTEND_BUCKET"]
    region = os.environ["AWS_REGION"]
    url = os.environ["PUBLIC_APP_URL"]
    for value, pattern in (
        (release, r"[a-f0-9]{40}"), (instance, r"i-[a-f0-9]+"),
        (bucket, r"[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]"),
        (region, r"[a-z]{2}-[a-z]+-\d"), (url, r"https://[a-zA-Z0-9.-]+"),
    ):
        if not re.fullmatch(pattern, value):
            raise ValueError("Invalid deployment parameter")
    checksum = hashlib.sha256(Path("runtime.tar.gz").read_bytes()).hexdigest()
    # SSM executes as root. Download as root, but run Compose as the existing
    # ubuntu user so ECR credentials and filesystem ownership remain consistent.
    command = f"""#!/bin/bash
set -euo pipefail
umask 077
install -d -o ubuntu -g ubuntu -m 755 /opt/markethub /opt/markethub/releases
exec 8>/opt/markethub/prepare.lock
flock -w 60 8
stage=$(mktemp -d /opt/markethub/releases/.incoming.XXXXXX)
trap 'rm -rf -- "$stage"' EXIT
aws s3 cp {shlex.quote(f's3://{bucket}/releases/{release}/runtime.tar.gz')} "$stage/runtime.tar.gz" --region {shlex.quote(region)} --only-show-errors
echo '{checksum}  '"$stage/runtime.tar.gz" | sha256sum --check --status
destination=/opt/markethub/releases/{release}
if [ -d "$destination" ]; then
  test "$(cat "$destination/.archive-sha256")" = {checksum}
else
  mkdir "$stage/files"
  tar -xzf "$stage/runtime.tar.gz" -C "$stage/files" --no-same-owner
  echo {checksum} > "$stage/files/.archive-sha256"
  chown -R ubuntu:ubuntu "$stage/files"
  mv "$stage/files" "$destination"
fi
flock -u 8
sudo -u ubuntu -H env RELEASE_TAG={release} AWS_REGION={shlex.quote(region)} PUBLIC_APP_URL={shlex.quote(url)} bash "$destination/production/aws/deploy.sh"
"""
    parameters = {"commands": ["bash <<'MARKETHUB_RELEASE'\n" + command + "\nMARKETHUB_RELEASE"],
                  "executionTimeout": ["2400"]}
    result = aws("ssm", "send-command", "--region", region,
                 "--instance-ids", instance, "--document-name", "AWS-RunShellScript",
                 "--comment", f"MarketHub {release}", "--timeout-seconds", "120",
                 "--parameters", json.dumps(parameters))
    command_id = result["Command"]["CommandId"]
    print(f"SSM command: {command_id}", flush=True)
    deadline = time.monotonic() + 2550
    while time.monotonic() < deadline:
        response = subprocess.run([
            "aws", "ssm", "get-command-invocation", "--region", region,
            "--command-id", command_id, "--instance-id", instance, "--output", "json",
        ], text=True, capture_output=True)
        if response.returncode:
            if "InvocationDoesNotExist" not in response.stderr:
                sys.exit(response.stderr)
        else:
            invocation = json.loads(response.stdout)
            status = invocation["Status"]
            if status not in ("Pending", "InProgress", "Delayed", "Cancelling"):
                print(invocation.get("StandardOutputContent", ""))
                print(invocation.get("StandardErrorContent", ""), file=sys.stderr)
                if status != "Success" or invocation["ResponseCode"] != 0:
                    sys.exit(f"EC2 deployment failed: {status}")
                return
        time.sleep(10)
    sys.exit(f"Timed out waiting for {command_id}; inspect SSM before retrying.")


if __name__ == "__main__":
    main()
