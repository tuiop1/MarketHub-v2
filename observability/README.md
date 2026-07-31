# Local observability

Observability is optional. A normal startup excludes Grafana, Prometheus,
Loki, Alloy, and Tempo, and disables OTLP trace export:

```bash
docker compose up -d
```

## Start with observability and traces

Both of the following are required:

- Enable the Compose profile with `--profile observability`.
- Set `OTLP_TRACING_ENABLED=true` so the backend services export traces to
  Tempo.

Run this command from the repository root:

```bash
OTLP_TRACING_ENABLED=true docker compose --profile observability up -d
```

Using only the profile starts the observability containers but leaves trace
export disabled. Setting only `OTLP_TRACING_ENABLED=true` enables the trace
exporters without starting Tempo.

The local interfaces are:

- Grafana: <http://localhost:3001>
- Prometheus: <http://localhost:9090>
- Loki: <http://localhost:3100>
- Alloy: <http://localhost:12345>
- Tempo: <http://localhost:3200>

## Return to application-only mode

Stop only the observability containers, then run the normal startup command so
the backend containers use the default `OTLP_TRACING_ENABLED=false` setting:

```bash
docker compose stop prometheus loki alloy grafana tempo
docker compose up -d
```

The named observability volumes are preserved, so collected local data remains
available the next time the profile is enabled.
