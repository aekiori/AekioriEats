package com.delivery.point.service.point;

import com.delivery.point.domain.point.PointBalance;
import com.delivery.point.domain.point.PointLedger;
import com.delivery.point.dto.request.ChargePointRequest;
import com.delivery.point.dto.response.PointBalanceResponse;
import com.delivery.point.repository.point.PointBalanceRepository;
import com.delivery.point.repository.point.PointLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointBalanceRepository pointBalanceRepository;
    private final PointLedgerRepository pointLedgerRepository;

    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long userId) {
        validateUserId(userId);
        Integer balance = pointBalanceRepository.findByUserId(userId)
            .map(PointBalance::getBalance)
            .orElse(0);
        return new PointBalanceResponse(userId, balance);
    }

    @Transactional
    public PointBalanceResponse charge(Long userId, ChargePointRequest request) {
        validateUserId(userId);
        if (request == null || request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive.");
        }

        PointBalance balance = pointBalanceRepository.findByUserId(userId)
            .orElseGet(() -> pointBalanceRepository.save(PointBalance.zero(userId)));

        balance.charge(request.amount());
        pointLedgerRepository.save(PointLedger.charged(
            userId,
            request.amount(),
            "point-charge:" + UUID.randomUUID(),
            request.reason()
        ));

        return new PointBalanceResponse(userId, balance.getBalance());
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be positive.");
        }
    }
}
