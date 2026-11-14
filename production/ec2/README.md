# Single-EC2 deployment

This is the original production deployment: Angular/Caddy, PostgreSQL, all
backend services, and optional observability run on one host.

The Compose file was moved here without changing its contents. Use the wrapper
from any working directory so relative build contexts and bind mounts still
resolve against the repository root. The project and volume names are unchanged.

```bash
cp .env.prod.example .env.prod
chmod 600 .env.prod
# Fill in .env.prod before starting.
bash production/ec2/compose.sh config --quiet
bash production/ec2/compose.sh up -d --build
bash production/ec2/compose.sh --profile observability up -d --build
bash production/ec2/compose.sh ps
```

Set `EC2_ENV_FILE` to use a different environment file. Do not run this stack
alongside the AWS stack on the same host: both publish the public HTTP/HTTPS
ports. Do not use `down -v` when you need to preserve data.

The existing `deployproduction.yml` workflow continues to deploy this option.
See [the deployment guide](../../DEPLOYMENT.md) for the full instructions.
