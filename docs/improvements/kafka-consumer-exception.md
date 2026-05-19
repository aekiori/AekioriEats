# Kafka Consumer 예외 처리 개선 메모

## 현재 상태

Kafka consumer 실패는 서비스별 `KafkaConsumerException`으로 감싼다.

단순히 `RuntimeException`을 던지는 것보다 낫다.
로그, 트레이싱, 에러 수집 도구에서 예외 타입만 봐도 "Kafka consumer 경계에서 실패했다"는 것을 구분할 수 있기 때문이다.

## 추후 개선 아이디어

`KafkaConsumerException`에 consumer 문맥 필드를 추가한다.

```java
public class KafkaConsumerException extends RuntimeException {
    private final String topic;
    private final int partition;
    private final long offset;
    private final String eventType;
}
```

기대 효과:

- consumer 실패를 예외 타입 기준으로 필터링할 수 있다.
- `topic`, `partition`, `offset`, `eventType`을 observability tag로 붙일 수 있다.
- DLT/재시도 원인 분석이 쉬워진다.
- 원래 실패 원인은 cause chain에 그대로 남길 수 있다.

## 구현 방향 후보

`ConsumerRecord`를 받는 static factory를 추가한다.

```java
public static KafkaConsumerException from(
    ConsumerRecord<?, ?> record,
    String eventType,
    String message,
    Throwable cause
) {
    return new KafkaConsumerException(
        message,
        cause,
        record.topic(),
        record.partition(),
        record.offset(),
        eventType
    );
}
```

지금 cleanup 범위에서는 단순 wrapper로 두고,
추후 모니터링/트레이싱 개선 단계에서 확장한다.
