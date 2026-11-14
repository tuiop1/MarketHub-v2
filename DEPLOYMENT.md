# MarketHub deployment guide

For a deployment with all components on one EC2 instance or VPS:

| Option | Files | Guide |
| --- | --- | --- |
| Original single EC2 / VPS | `production/ec2/docker-compose.prod.yml` | [Original deployment](production/ec2/DEPLOYMENT.md) |

The original Compose file was moved without changing its contents. Use its
wrapper to preserve repository-root build/mount paths and existing volumes:

```bash
bash production/ec2/compose.sh up -d --build
```

The root `docker-compose.yml` is for local development. Additional AWS runtime
configuration is available under `production/aws/`. Keep each Compose variant
standalone; do not merge it with the local file or run two production variants
on the same host's HTTP/HTTPS ports simultaneously.
