# Production deployments

| Directory | Purpose | Database | Frontend |
| --- | --- | --- | --- |
| [ec2](ec2/README.md) | Original deployment on one server | PostgreSQL container | Angular served by Caddy on EC2 |
| `aws/` | EC2 backend with managed data and static hosting | Amazon RDS PostgreSQL | Private S3 bucket behind CloudFront |

The root `docker-compose.yml` remains the local development environment.
Each production variant is standalone; do not merge it with the local file.

Always use the variant's `compose.sh` wrapper. It fixes the project directory
at the repository root, keeping build contexts, configuration mounts and
environment-file locations predictable after this directory reorganization.
