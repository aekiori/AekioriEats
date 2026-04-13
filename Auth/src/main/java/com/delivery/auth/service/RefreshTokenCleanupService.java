package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.repository.token.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${auth.token.cleanup.batch-size:500}")
    private int batchSize;

    @Scheduled(cron = "${auth.token.cleanup.cron:0 0 5 * * *}")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        List<RefreshToken> targets = refreshTokenRepository.findExpiredOrRevoked(
            now,
            PageRequest.of(0, batchSize)
        );

        if (targets.isEmpty()) {
            return;
        }

        refreshTokenRepository.deleteAllInBatch(targets);
        log.info("RefreshToken cleanup completed. deleted={}, threshold={}", targets.size(), now);
    }
}
