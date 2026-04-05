package com.delivery.order.service;

import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.dto.response.OutboxReplayResultDto;
import com.delivery.order.dto.response.OutboxResultDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxAdminService {
    private static final Logger log = LoggerFactory.getLogger(OutboxAdminService.class);

    private final OutboxRepository outboxRepository;
    private final OutboxStatusService outboxStatusService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${order.outbox.smt-topic:outbox.event.ORDER}")
    private String outboxSmtTopic;

    @Transactional(readOnly = true)
    public List<OutboxResultDto> getOutboxes(String status) {
        Outbox.Status targetStatus = parseStatus(status);

        return outboxRepository.findByStatusOrderByCreatedAtAsc(targetStatus).stream()
            .map(this::toOutboxResultDto)
            .toList();
    }

    public OutboxReplayResultDto replayFailedOutbox(String eventId) {
        Outbox outbox = outboxRepository.findByEventId(eventId)
            .orElseThrow(() -> new ApiException(
                "OUTBOX_NOT_FOUND",
                "Outbox 이벤트를 찾을 수 없다.",
                HttpStatus.NOT_FOUND
            ));

        if (outbox.getStatus() != Outbox.Status.FAILED) {
            throw new ApiException(
                "OUTBOX_REPLAY_NOT_ALLOWED",
                "FAILED 상태의 Outbox만 재처리할 수 있다.",
                HttpStatus.CONFLICT
            );
        }

        try {
            outboxStatusService.resetToInit(eventId);

            kafkaTemplate.send(
                MessageBuilder.withPayload(outbox.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, outboxSmtTopic)
                    .setHeader(KafkaHeaders.KEY, outbox.getPartitionKey())
                    .setHeader("eventId", outbox.getEventId())
                    .setHeader("eventType", outbox.getEventType())
                    .setHeader("aggregateId", String.valueOf(outbox.getAggregateId()))
                    .build()
            ).get();

            log.info(
                "Outbox 수동 재처리 발행 완료. eventId={}, aggregateId={}, topic={}",
                outbox.getEventId(),
                outbox.getAggregateId(),
                outboxSmtTopic
            );

            return new OutboxReplayResultDto(
                outbox.getEventId(),
                Outbox.Status.INIT.name(),
                outboxSmtTopic,
                LocalDateTime.now()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            outboxStatusService.markFailed(eventId);

            log.error(
                "Outbox 수동 재처리 발행 실패. eventId={}, aggregateId={}, topic={}",
                outbox.getEventId(),
                outbox.getAggregateId(),
                outboxSmtTopic,
                exception
            );

            throw new ApiException(
                "OUTBOX_REPLAY_FAILED",
                "Outbox 수동 재처리에 실패했다.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private Outbox.Status parseStatus(String status) {
        try {
            return Outbox.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                "INVALID_OUTBOX_STATUS",
                "유효하지 않은 Outbox 상태다.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private OutboxResultDto toOutboxResultDto(Outbox outbox) {
        return new OutboxResultDto(
            outbox.getId(),
            outbox.getEventId(),
            outbox.getAggregateType(),
            outbox.getAggregateId(),
            outbox.getEventType(),
            outbox.getStatus().name(),
            outbox.getPartitionKey(),
            outbox.getCreatedAt()
        );
    }
}
