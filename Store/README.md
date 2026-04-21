# Store 서비스

상점 소유자와 상점 상태를 관리하는 도메인 서비스다.

## 주요 기능
- 상점 생성
- 상점 상세 조회
- 상점 상태 변경 (`OPEN`, `CLOSED`, `BREAK`)
- `X-User-Id` 기준 owner 본인만 수정 권한을 가진다
- `OrderCreated` 이벤트 소비 후 가게 주문 검증 결과 저장

`delivery_tip`은 지금 고정 금액으로만 동작함. 거리별 차등 배달비는 아직 미구현 상태임.

## 주문 검증 컨슈머
- 토픽: `outbox.event.OrderCreated`
- 이벤트 타입: `OrderCreated`
- 검증 규칙:
  - 가게 존재 여부
  - 가게 상태(`OPEN`) 여부
  - 주문 총액(`totalAmount`) >= 최소주문금액(`minOrderAmount`)
- 검증 결과는 `store_order_validation` 테이블에 저장한다.

```sql
CREATE TABLE store_order_validation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    result ENUM('ACCEPTED','REJECTED') NOT NULL,
    reject_code VARCHAR(50),
    reject_reason VARCHAR(200),
    validated_at DATETIME NOT NULL
);
```

## API

### Swagger
- UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`
- 기본은 공개 API로 표시되고, `/api/v1/owner/**` 엔드포인트만 Swagger 자물쇠(Authorize) 대상이다.

### 상점 생성
`POST /api/v1/stores`

요청 예시:
```json
{
  "ownerUserId": 1,
  "name": "Aekiori Chicken"
}
```

### 상점 조회
`GET /api/v1/stores/{storeId}`

### 상점 상태 변경
`PATCH /api/v1/stores/{storeId}/status`

요청 예시:
```json
{
  "status": "CLOSED"
}
```

## 테스트 실행
```cmd
.\gradlew.bat test
```

## 검색 설계 메모 (CQRS + Elasticsearch)
- 검색 목표는 사용자가 `치킨` 같은 키워드를 입력했을 때 상점 관련 정보를 통합 검색으로 보여주는 것이다.
- 검색 소스는 아래 3개다.
- `store name`: 예) `애기오리 양념치킨 산본점`
- `tag`: 이름에 키워드가 없어도 태그에 `치킨`이 있으면 검색 대상
- `menu name`: 이름/태그에 없어도 메뉴명에 `치킨`이 있으면 검색 대상
- 근본 이유는 정규화된 RDB 테이블(`stores`, `store_tags`, `menus`)을 검색용 문서로 비정규화해서 빠른 검색 UX를 만들기 위해서다.
- CQRS 기준으로 Command 모델은 MySQL 정규화 구조를 유지하고, Query 모델은 Elasticsearch 인덱스(검색 전용)로 분리한다.
- 관련도 가중치는 기본적으로 `store name > tag > menu` 순으로 가져간다.
