package com.delivery.point.service.event;

import com.delivery.point.domain.point.PointBalance;
import com.delivery.point.domain.point.PointLedger;
import com.delivery.point.dto.event.PointDeductionRequestedEventDto;
import com.delivery.point.repository.outbox.OutboxRepository;
import com.delivery.point.repository.point.PointBalanceRepository;
import com.delivery.point.repository.point.PointLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointDeductionRequestedEventHandler {
    private static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_POINT_BALANCE";
    private static final String INVALID_AMOUNT = "INVALID_POINT_AMOUNT";

    private final PointBalanceRepository pointBalanceRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final OutboxRepository outboxRepository;
    private final PointDeductionResultOutboxFactory outboxFactory;

    @Transactional
    public void handle(PointDeductionRequestedEventDto event) {
        String idempotencyKey = "point-deduct:" + event.eventId();
        if (pointLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        if (event.amount() == null || event.amount() <= 0) {
            saveFailed(event, idempotencyKey, INVALID_AMOUNT);
            return;
        }

        PointBalance balance = pointBalanceRepository.findByUserId(event.userId())
            .orElseGet(() -> pointBalanceRepository.save(PointBalance.zero(event.userId())));

        if (!balance.canDeduct(event.amount())) {
            saveFailed(event, idempotencyKey, INSUFFICIENT_BALANCE);
            return;
        }

        balance.deduct(event.amount());
        pointLedgerRepository.save(PointLedger.deductionSucceeded(
            event.userId(),
            event.orderId(),
            event.amount(),
            idempotencyKey
        ));
        outboxRepository.save(outboxFactory.deducted(
            event.orderId(),
            event.paymentId(),
            event.userId(),
            event.amount()
        ));
    }

    private void saveFailed(PointDeductionRequestedEventDto event, String idempotencyKey, String reason) {
        pointLedgerRepository.save(PointLedger.deductionFailed(
            event.userId(),
            event.orderId(),
            event.amount() == null ? 0 : event.amount(),
            idempotencyKey,
            reason
        ));
        outboxRepository.save(outboxFactory.failed(
            event.orderId(),
            event.paymentId(),
            event.userId(),
            event.amount() == null ? 0 : event.amount(),
            reason
        ));
    }
}
