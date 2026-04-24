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

Prometheus ([prometheus.yml](../../infra/observability/prometheus/prometheus.yml))
- `global.scrape_interval`: 메트릭 수집 주기. 현재 `15s`.
  - 로컬에서 응답 지연이나 에러율 변화를 너무 느리게 보지 않으면서도, 노트북 자원을 과하게 잡아먹지 않는 절충값으로 잡았다.
- `global.evaluation_interval`: 룰 평가 주기. 현재 `15s`.
  - 지금은 무거운 알림 룰이 거의 없어서 `scrape_interval`과 같은 간격으로 맞춰, 그래프 해석과 룰 평가 타이밍이 어긋나지 않게 했다.
- `scrape_configs[].job_name`: Grafana/PromQL에서 필터링할 서비스 이름(`job` 라벨).
  - 대시보드와 PromQL에서 `job="order"`, `job="payment"`처럼 바로 서비스별 필터가 가능하게 하려고 서비스명 단위로 나눴다.
- `scrape_configs[].metrics_path`: 메트릭 엔드포인트 경로. Spring은 보통 `/actuator/prometheus`.
  - Spring Boot Actuator 기본 관례를 그대로 써서 서비스별 설정 차이를 줄이고, 메트릭 노출 위치를 추측하지 않게 했다.
- `scrape_configs[].targets`: 실제 수집 대상 주소.
  - 로컬 Docker 안 Prometheus가 호스트에서 직접 띄운 앱을 긁어가야 해서 `host.docker.internal:{port}` 기준으로 맞췄다.

Grafana ([dashboard.yml](../../infra/observability/grafana/provisioning/dashboards/dashboard.yml))
- `providers[].folder`: 대시보드가 들어갈 Grafana 폴더명.
  - 대시보드가 늘어나도 Aekiori 관련 리소스를 한 폴더에 모아 찾기 쉽게 하려고 분리했다.
- `providers[].updateIntervalSeconds`: JSON 파일 변경 감지 주기.
  - 현재 `10s`로 두어 로컬에서 대시보드 JSON 수정 후 거의 바로 반영되게 하되, 너무 잦은 polling으로 시끄럽지 않게 잡았다.
- `providers[].allowUiUpdates`: UI 수정 허용 여부.
  - 로컬에서 패널을 바로 만져보며 실험하는 흐름이 많아서 켜뒀다. 완전 불변 인프라보다 탐색/튜닝 편의성을 우선한 설정이다.
- `providers[].disableDeletion`: 파일 삭제 시 Grafana에서도 삭제 동기화할지 여부.
  - 현재 `false`로 두어 파일 기반 상태를 기준으로 맞추게 했다. 로컬에서도 "파일엔 없는데 UI에만 남아있는 대시보드"가 쌓이지 않게 하려는 의도다.
- `providers[].options.path`: 컨테이너 내부에서 JSON을 읽는 경로.
  - Dockerfile에서 복사한 대시보드 위치와 정확히 맞춰, provisioning이 별도 마운트 없이도 일관되게 동작하게 했다.
