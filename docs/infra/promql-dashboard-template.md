# 도메인 대시보드 PromQL 템플릿

Grafana 패널에서 서비스명을 고정하지 않고 재사용하려면 아래 템플릿으로 쓰면 된다.

## 변수
- `$svc_name`: `order`, `user`, `auth`, `gateway` 같은 job 이름

## Grafana 변수 설정 (멀티셀렉)
`svc_name` 변수를 멀티셀렉으로 쓰려면 아래처럼 두면 된다.

1. `Dashboard settings -> Variables -> Add variable`
2. `Name`: `svc_name`
3. `Type`: `Query`
4. `Data source`: `Prometheus`
5. `Query type`: `Label values`
6. `Metric`: `up`
7. `Label`: `job`
8. (선택) `Regex`: `/gateway|auth|user|order/`
9. `Multi-value`: ON
10. `Include All option`: ON

중요:
- 패널 쿼리는 `job="$svc_name"`가 아니라 `job=~"$svc_name"` 형태로 써야 멀티셀렉/All이 정상 동작한다.

## 패널 구성 추천 (실무형)
모든 지표를 `Stat + Time series`로 2배로 늘릴 필요는 없다.

- 상단 `Stat` 4~6개: `Service UP`, `Request RPS`, `p95`, `5xx ratio`, `4xx ratio`
- 중단 `Time series` 3~4개: `Request RPS by URI`, `p95 by URI`, `5xx ratio`
- 하단 `Time series` 3~4개: `JVM Heap`, `CPU`, `GC Pause Count`, `GC Pause Time`

보통 한 도메인 대시보드는 총 8~12개 패널이면 충분하다.

## 패널별 튜닝 포인트

### Service UP
- Value mapping 권장: `1 -> UP(초록)`, `0 -> DOWN(빨강)`

### Request RPS by URI
- Legend를 `{{uri}}`로 두면 핫 엔드포인트 추적이 쉽다.
- URI 종류가 많아지면 `topk(10, ...)`로 제한해서 보는 걸 권장한다.

### Latency (p50/p95/p99 같이 보기)
같은 패널에 아래 3개를 함께 넣으면 tail latency 변화가 잘 보인다.

```promql
histogram_quantile(0.50, sum by (le) (rate(http_server_requests_seconds_bucket{job=~"$svc_name"}[5m])))
```

```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job=~"$svc_name"}[5m])))
```

```promql
histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{job=~"$svc_name"}[5m])))
```

### Error Ratio
- 패널 단위는 `Percent (0.0-1.0)` 사용 권장
- Threshold 예시: `1%` 경고, `5%` 위험

### JVM Heap
Heap 절대값뿐 아니라 사용률도 같이 보는 걸 권장한다.

```promql
sum(jvm_memory_used_bytes{job=~"$svc_name",area="heap"}) /
clamp_min(sum(jvm_memory_max_bytes{job=~"$svc_name",area="heap"}), 1)
```

### CPU
현재는 평균값을 보지만, replica가 늘면 인스턴스별 분리 관찰도 필요하다.

```promql
avg by (instance) (process_cpu_usage{job=~"$svc_name"}) * 100
```

### GC
- `GC Pause Count`와 `GC Pause Time`은 `CPU` 패널 옆에 두는 게 분석에 유리하다.
- 부하 시점에 GC/CPU가 같이 튀는지 바로 비교 가능하다.

## 윈도우(5m) 사용 이유
- `5m`는 반응성과 안정성의 균형을 맞춘 기본값이다.
- 실시간 부하 확인은 `1m~2m`, 운영 기본은 `5m`, 장기 추세는 `10m~30m`로 조정하면 된다.

## Grafana 패널 타입 권장
- `Stat`: 현재값 카드 (Instant ON 권장)
- `Time series`: 추이 그래프 (Instant OFF 권장)

## 공통 템플릿

### 1) Service UP
```promql
sum(up{job=~"$svc_name"}) or vector(0)
```

### 2) Request RPS
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[1m])) or vector(0)
```

### 3) Request RPS by URI
```promql
topk(10,
  sum by (uri) (rate(http_server_requests_seconds_count{
    job=~"$svc_name",
    uri=~"/api/v1/.*",
    uri!="/actuator/prometheus"
  }[1m]))
)
```

### 4) p95 Latency (전체)
```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job=~"$svc_name"}[5m])))
```

### 5) p95 Latency by URI
```promql
topk(10,
  histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{
    job=~"$svc_name",
    uri=~"/api/v1/.*",
    uri!="/actuator/prometheus"
  }[5m])))
)
```

### 6) 5xx Error Ratio
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name",status=~"5.."}[5m])) /
clamp_min(sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[5m])), 1)
```

### 7) 4xx Error Ratio
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name",status=~"4.."}[5m])) /
clamp_min(sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[5m])), 1)
```

### 8) JVM Heap Used
```promql
sum(jvm_memory_used_bytes{job=~"$svc_name",area="heap"})
```

### 9) CPU Usage (%)
```promql
avg(process_cpu_usage{job=~"$svc_name"}) * 100
```

### 10) GC Pause Count (5m)
```promql
sum(increase(jvm_gc_pause_seconds_count{job=~"$svc_name"}[5m])) or vector(0)
```

### 11) GC Pause Time (ms, 5m)
```promql
sum(increase(jvm_gc_pause_seconds_sum{job=~"$svc_name"}[5m])) * 1000 or vector(0)
```

## 쿼리 해석 (우리 프로젝트 기준)

### 공통 문법 먼저
- `job=~"$svc_name"`: 선택된 서비스들(멀티셀렉)을 정규식으로 매칭
- `[1m]`, `[5m]`: 최근 1분/5분 구간으로 계산
- `rate(counter[window])`: window 구간 기준 초당 평균 증가량
- `increase(counter[window])`: window 구간 누적 증가량
- `histogram_quantile(0.95, ...)`: 히스토그램 버킷으로 p95 계산
- `topk(10, ...)`: 값이 큰 상위 10개만 표시
- `clamp_min(x, 1)`: 분모 0 방지
- `... or vector(0)`: 시계열이 없을 때 0으로 대체

### 1) Service UP
```promql
sum(up{job=~"$svc_name"}) or vector(0)
```
- 의미: 현재 살아있는 타깃 수
- 예: `svc_name=order|user|auth|gateway`면 최대 4
- 용도: 서비스 생존 확인

### 2) Request RPS
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[1m])) or vector(0)
```
- 의미: 최근 1분 기준 전체 API 초당 요청 수
- 용도: 트래픽 추세, 부하 시작 시점 확인

### 3) Request RPS by URI
```promql
topk(10,
  sum by (uri) (rate(http_server_requests_seconds_count{
    job=~"$svc_name",
    uri=~"/api/v1/.*",
    uri!="/actuator/prometheus"
  }[1m]))
)
```
- 의미: URI별 RPS 상위 10개
- 용도: 어떤 API가 트래픽 대부분을 먹는지 파악

### 4) p95 Latency (전체)
```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job=~"$svc_name"}[5m])))
```
- 의미: 최근 5분 기준 전체 p95 응답시간 (단위: 초)
- 용도: 사용자 체감 지연(꼬리 지연) 감시

### 5) p95 Latency by URI
```promql
topk(10,
  histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{
    job=~"$svc_name",
    uri=~"/api/v1/.*",
    uri!="/actuator/prometheus"
  }[5m])))
)
```
- 의미: URI별 p95 상위 10개
- 용도: 느린 엔드포인트 우선순위 도출

### 6) 5xx Error Ratio
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name",status=~"5.."}[5m])) /
clamp_min(sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[5m])), 1)
```
- 의미: 전체 요청 중 서버 에러(5xx) 비율
- 용도: 장애/회귀 감지

### 7) 4xx Error Ratio
```promql
sum(rate(http_server_requests_seconds_count{job=~"$svc_name",status=~"4.."}[5m])) /
clamp_min(sum(rate(http_server_requests_seconds_count{job=~"$svc_name"}[5m])), 1)
```
- 의미: 전체 요청 중 클라이언트 에러(4xx) 비율
- 용도: 인증/인가/입력 검증 문제 감지

### 8) JVM Heap Used
```promql
sum(jvm_memory_used_bytes{job=~"$svc_name",area="heap"})
```
- 의미: 힙 사용량(바이트)
- 용도: 메모리 압박/OOM 징조 확인

### 9) CPU Usage (%)
```promql
avg(process_cpu_usage{job=~"$svc_name"}) * 100
```
- 의미: 프로세스 CPU 평균 사용률(%)
- 용도: 부하 시 CPU 병목 확인

### 10) GC Pause Count (5m)
```promql
sum(increase(jvm_gc_pause_seconds_count{job=~"$svc_name"}[5m])) or vector(0)
```
- 의미: 최근 5분 GC pause 발생 횟수
- 용도: GC 빈도 급증 감시

### 11) GC Pause Time (ms, 5m)
```promql
sum(increase(jvm_gc_pause_seconds_sum{job=~"$svc_name"}[5m])) * 1000 or vector(0)
```
- 의미: 최근 5분 GC pause 누적 시간(ms)
- 용도: STW로 인한 지연 영향 파악

## 0 / No data 체크리스트

### 1) 윈도우 구간 내 트래픽 자체가 없는지
- `rate(...[1m])`는 1분 동안 증가가 거의 없으면 0이 정상
- 로컬/저트래픽 환경은 `[5m]` 또는 `[10m]`로 늘려서 확인

### 2) 변수 매칭 연산자 확인
- 멀티셀렉 변수는 `job="$svc_name"`가 아니라 `job=~"$svc_name"` 사용

### 3) Instant/Range 설정 확인
- `Stat` 패널: `Instant ON` 권장
- `Time series` 패널: `Instant OFF` 권장

### 4) 필터가 너무 강한지
- `uri=~"/api/v1/.*"` 조건 때문에 actuator/기타 경로가 제외될 수 있음
- `uri!="/actuator/prometheus"`를 넣었으면 해당 호출은 당연히 집계에서 빠짐

### 5) 실제 타깃이 살아있는지
- Prometheus `up{job="..."}` 값이 1인지 먼저 확인
- `DOWN`이면 메트릭 수집 자체가 안 됨

### 6) 대시보드 갱신 주기 확인
- Grafana 우상단 Auto refresh 꺼져 있으면 값이 안 변하는 것처럼 보임

### 7) 카운터 리셋 영향
- 앱 재시작 직후 `rate`가 잠깐 불안정할 수 있음
- 1~2분 지나서 다시 확인
