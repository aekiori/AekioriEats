FROM prom/prometheus:v2.54.1

COPY infra/observability/prometheus/prometheus.yml /etc/prometheus/prometheus.yml
