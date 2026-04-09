FROM grafana/grafana:11.2.0

COPY --chown=grafana:grafana infra/observability/grafana/provisioning/datasources /etc/grafana/provisioning/datasources
COPY --chown=grafana:grafana infra/observability/grafana/provisioning/dashboards /etc/grafana/provisioning/dashboards
COPY --chown=grafana:grafana infra/observability/grafana/dashboards /var/lib/grafana/dashboards
