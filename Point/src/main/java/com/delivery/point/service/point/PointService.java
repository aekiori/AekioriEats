package com.delivery.point.service.point;

import com.delivery.point.domain.point.PointBalance;
import com.delivery.point.domain.point.PointLedger;
import com.delivery.point.dto.request.ChargePointRequestDto;
import com.delivery.point.dto.response.PointBalanceResponseDto;
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
    private final PointAuthorizationService pointAuthorizationService;

    @Transactional(readOnly = true)
    public PointBalanceResponseDto getBalance(Long userId, long authenticatedUserId) {
        validateUserId(userId);
        pointAuthorizationService.requireSelf(authenticatedUserId, userId);
        Integer balance = pointBalanceRepository.findByUserId(userId)
            .map(PointBalance::getBalance)
            .orElse(0);
        return new PointBalanceResponseDto(userId, balance);
    }

    @Transactional
    public PointBalanceResponseDto charge(Long userId, ChargePointRequestDto request, long authenticatedUserId) {
        validateUserId(userId);
        pointAuthorizationService.requireSelf(authenticatedUserId, userId);
        if (request == null || request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive.");
        }

        PointBalance balance = pointBalanceRepository.findByUserId(userId)
            .orElseGet(() -> pointBalanceRepository.save(PointBalance.zero(userId)));

        balance.charge(request.amount());
        recordPointCharge(userId, request);

        return new PointBalanceResponseDto(userId, balance.getBalance());
    }

    private void recordPointCharge(Long userId, ChargePointRequestDto request) {
        pointLedgerRepository.save(PointLedger.charged(
            userId,
            request.amount(),
            "point-charge:" + UUID.randomUUID(),
            request.reason()
        ));
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be positive.");
        }
    }
}
