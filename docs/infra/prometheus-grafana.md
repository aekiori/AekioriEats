# Prometheus + Grafana (로컬)

## 구성
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`Aekiori` / `CuteAekiori`)

## 실행
루트에서:

```cmd
docker compose --env-file infra/docker/infra/.env.infra -f infra/docker/infra/compose.infra.yml up -d prometheus grafana
```

전체 스택이면:

```cmd
docker compose --env-file infra/docker/infra/.env.infra -f infra/docker/infra/compose.infra.yml up -d --build
```

## 스크랩 대상
Prometheus가 아래 엔드포인트를 수집한다.

- `http://host.docker.internal:8088/actuator/prometheus` (Gateway)
- `http://host.docker.internal:8084/actuator/prometheus` (Auth)
- `http://host.docker.internal:8082/actuator/prometheus` (User)
- `http://host.docker.internal:8081/actuator/prometheus` (Order)
- `http://host.docker.internal:8085/actuator/prometheus` (Store)
- `http://host.docker.internal:8086/actuator/prometheus` (Payment)
- `http://host.docker.internal:8087/actuator/prometheus` (Point)

주의:
- 로컬에서 각 서비스가 떠 있어야 `UP`으로 잡힌다.
- 서비스가 내려가 있으면 Prometheus `Targets` 화면에서 `DOWN`으로 보이는 게 정상이다.

## 빠른 확인 포인트
Prometheus:
- `up`
- `jvm_memory_used_bytes`
- `http_server_requests_seconds_count`

Grafana:
- 데이터소스는 자동으로 `Prometheus`가 등록된다.
- 기본 대시보드 `Aekiori Overview`가 자동으로 provision 된다.
- 경로: `Dashboards -> Aekiori -> Aekiori Overview`
- 필요하면 여기에 패널을 계속 추가해서 확장하면 된다.

## 보관 정책 (Retention)
현재 `infra/docker/infra/compose.infra.yml`에서 Prometheus TSDB 보관 정책을 아래처럼 적용했다.

- `--storage.tsdb.retention.time=7d`
- `--storage.tsdb.retention.size=5GB`

의미:
- 최근 7일 데이터까지만 유지
- 또는 총 용량이 5GB를 넘기지 않도록 상한 유지
- 둘 중 먼저 걸리는 조건으로 오래된 데이터가 정리된다.

## 주요 설정값 설명

Prometheus ([prometheus.yml](../infra/observability/prometheus/prometheus.yml))
- `global.scrape_interval`: 메트릭 수집 주기. 현재 `15s`.
- `global.evaluation_interval`: 룰 평가 주기. 현재 `15s`.
- `scrape_configs[].job_name`: Grafana/PromQL에서 필터링할 서비스 이름(`job` 라벨).
- `scrape_configs[].metrics_path`: 메트릭 엔드포인트 경로. Spring은 보통 `/actuator/prometheus`.
- `scrape_configs[].targets`: 실제 수집 대상 주소.

Grafana ([dashboard.yml](../infra/observability/grafana/provisioning/dashboards/dashboard.yml))
- `providers[].folder`: 대시보드가 들어갈 Grafana 폴더명.
- `providers[].updateIntervalSeconds`: JSON 파일 변경 감지 주기.
- `providers[].allowUiUpdates`: UI 수정 허용 여부.
- `providers[].disableDeletion`: 파일 삭제 시 Grafana에서도 삭제 동기화할지 여부.
- `providers[].options.path`: 컨테이너 내부에서 JSON을 읽는 경로.
