# Store 도메인 설계 정리

## 1. 목표
- 배달 도메인에서 `Store`를 단순 CRUD가 아니라 확장 가능한 구조로 설계한다.
- `Store`, `Menu`, `Category`, `Tag`, `Order`가 나중에 늘어나도 정합성과 운영성을 유지한다.
- 검색은 CQRS로 분리하고, 조회 성능/유연성은 Elasticsearch로 가져간다.

## 2. 핵심 모델 분리
- `Store -> Category`는 분류성 데이터다. 예: 치킨, 한식, 야식.
- `Menu -> Tag`는 속성성 데이터다. 예: 베스트, 신메뉴, 매운맛.
- 둘은 성격이 달라서 테이블을 분리한다.
- `Tag` 하나에 `type(STORE/MENU)`를 두는 통합 모델은 초반엔 쉬워도 중장기적으로 지저분해지기 쉬워서 지양한다.

## 3. 관계 설계
- `store`와 `category`는 N:M 관계다.
- `menu`와 `tag`도 N:M 관계다.
- 따라서 중간 테이블이 필수다.
- `store_category` PK는 `(store_id, category_id)` 복합키를 권장한다.
- `menu_tag` PK는 `(menu_id, tag_id)` 복합키를 권장한다.

## 4. DDL 초안
```sql
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE store_category (
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (store_id, category_id)
);

CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE menu_tag (
    menu_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (menu_id, tag_id)
);
```

## 5. 영업 상태/운영시간 설계
- `store` 테이블에는 운영 핵심 상태를 둔다.
- 권장 컬럼: `status`, `status_override`, `min_order_amount`.
- `status` 값 예: `OPEN`, `CLOSED`, `BREAK`.
- 운영시간과 휴무는 별도 테이블로 분리한다.
- `store_hours`는 요일별 운영시간을 관리한다.
- `store_holiday`는 특정일 임시휴무를 관리한다.

컬럼 의미
- `status`: 현재 노출 영업 상태다. (`OPEN`, `CLOSED`, `BREAK`)
- `status_override`: 수동 상태 오버라이드 여부다.
- `status_override = true`이면 스케줄러가 자동으로 상태를 바꾸지 않는다.
- `min_order_amount`: 최소 주문 금액(원)이다. 장바구니 합계가 이 값보다 작으면 주문 생성을 막는다.

```sql
ALTER TABLE store ADD COLUMN status ENUM('OPEN','CLOSED','BREAK') NOT NULL DEFAULT 'CLOSED';
ALTER TABLE store ADD COLUMN status_override BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE store ADD COLUMN min_order_amount INT NOT NULL DEFAULT 0;

CREATE TABLE store_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE store_holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    holiday_date DATE NOT NULL,
    reason VARCHAR(100)
);
```

## 6. 상태 우선순위 규칙
- 운영 우선순위는 `수동 override > 휴무일 > 운영시간 자동`으로 둔다.
- 사장이 `BREAK`를 누르면 `status_override = true`로 두고 스케줄러가 상태를 덮어쓰지 않게 한다.
- 사장이 자동모드로 복귀하면 `status_override = false`로 되돌린다.
- 자동 스케줄러는 override가 꺼진 가게만 `OPEN/CLOSED`를 시간 기반으로 갱신한다.

## 7. 메뉴 옵션 모델
- 메뉴 옵션은 `Menu -> MenuOptionGroup -> MenuOption` 3계층으로 간다.
- `MenuOptionGroup`이 없는 단일 `menu_option` 구조는 검증이 무너진다.
- 그룹 기준으로 필수/복수선택/최소/최대 선택 개수를 강제할 수 있어야 한다.

권장 컬럼
- `menu_option_group`: `is_required`, `is_multiple`, `min_select_count`, `max_select_count`, `display_order`.
- `menu_option`: `extra_price`, `is_available`, `display_order`.

## 8. 주문 시 스냅샷 저장
- 주문 시점의 메뉴/옵션 이름과 가격을 주문 테이블에 스냅샷으로 저장한다.
- 나중에 메뉴 가격이 바뀌어도 과거 주문 정합성을 유지할 수 있다.
- `menu_id`/`option_id` 참조만으로 과거 주문 금액을 재계산하는 방식은 지양한다.

```sql
CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    base_price INT NOT NULL,
    quantity INT NOT NULL,
    total_price INT NOT NULL
);

CREATE TABLE order_item_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_item_id BIGINT NOT NULL,
    option_group_name VARCHAR(100) NOT NULL,
    option_name VARCHAR(100) NOT NULL,
    extra_price INT NOT NULL
);
```

## 9. 검색 구조 (CQRS + Elasticsearch)
- 검색 목표는 키워드 입력 시 `store name`, `category/tag`, `menu name`을 통합 검색하는 것이다.
- Command 모델은 MySQL 정규화 구조를 유지한다.
- Query 모델은 Elasticsearch 비정규화 문서로 분리한다.
- 기본 가중치 전략은 `store name > category/tag > menu` 순으로 둔다.

문서 예시
```json
{
  "storeId": 1,
  "storeName": "애기오리 닭발 산본점",
  "categories": ["치킨", "야식"],
  "tags": ["맛집"],
  "menuNames": ["마왕닭발", "후라이드치킨"]
}
```

## 10. 안티패턴 체크리스트
- `tag(type=STORE|MENU)` 통합 모델을 초반부터 채택.
- `option_group` 없이 `option` 단일 테이블만 사용.
- 주문 시 스냅샷 없이 실시간 메뉴 가격 재참조 계산.
- 초기부터 공용 옵션 템플릿 재사용 구조를 과도하게 설계.

## 11. 현재 단계 권장안
- 지금 단계에서는 메뉴 소유형 옵션 구조를 유지한다.
- 공용 옵션 템플릿은 실제 재사용 요구가 반복될 때 도입한다.
- 먼저 정합성 있는 주문/검색 흐름을 완성하고, 이후 재사용/운영 자동화로 확장한다.
