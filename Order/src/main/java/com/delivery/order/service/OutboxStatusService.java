package com.delivery.order.service;

import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxStatusService {
    private final OutboxRepository outboxRepository;

    @Transactional
    public void markPublished(String eventId) {
        Outbox outbox = findOutbox(eventId);
        outbox.markPublished();
    }

    @Transactional
    public void markFailed(String eventId) {
        Outbox outbox = findOutbox(eventId);
        outbox.markFailed();
    }

    @Transactional
    public void resetToInit(String eventId) {
        Outbox outbox = findOutbox(eventId);
        outbox.updateStatus(Outbox.Status.INIT);
    }

    @Transactional
    public boolean markPublishedIfInit(String eventId) {
        return outboxRepository.updateStatusIfCurrent(
            eventId,
            Outbox.Status.INIT,
            Outbox.Status.PUBLISHED
        ) > 0;
    }

    private Outbox findOutbox(String eventId) {
        return outboxRepository.findByEventId(eventId)
            .orElseThrow(() -> new ApiException(
                "OUTBOX_NOT_FOUND",
                "Outbox 이벤트를 찾을 수 없다.",
                HttpStatus.NOT_FOUND
            ));
    }
}
