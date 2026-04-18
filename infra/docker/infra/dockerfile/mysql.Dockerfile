FROM mysql:8.0

COPY infra/mysql/init /docker-entrypoint-initdb.d
