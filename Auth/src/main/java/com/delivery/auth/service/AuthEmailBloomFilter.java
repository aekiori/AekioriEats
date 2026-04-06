package com.delivery.auth.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AuthEmailBloomFilter {
    private static final String LOAD_SQL = """
        SELECT user_id, email
        FROM auth_users
        WHERE user_id > ?
        ORDER BY user_id ASC
        LIMIT ?
        """;

    private final JdbcTemplate jdbcTemplate;
    private final BloomFilter<CharSequence> bloomFilter;
    private final boolean enabled;
    private final int warmupBatchSize;
    private final AtomicBoolean warmedUp = new AtomicBoolean(false);

    public AuthEmailBloomFilter(
        JdbcTemplate jdbcTemplate,
        @Value("${auth.bloom.enabled:true}") boolean enabled,
        @Value("${auth.bloom.expected-insertions:5000000}") long expectedInsertions,
        @Value("${auth.bloom.fpp:0.01}") double fpp,
        @Value("${auth.bloom.warmup-batch-size:10000}") int warmupBatchSize
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.warmupBatchSize = warmupBatchSize;
        this.bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            expectedInsertions,
            fpp
        );

        // enabled=false면 warmUp()이 early return 하므로 warmedUp=true 로 세팅해 DB fallback 방지
        this.warmedUp.set(!enabled);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUp() {
        if (!enabled) {
            return;
        }

        long startedAt = System.currentTimeMillis();

        try {
            long lastUserId = 0L;
            long loaded = 0L;

            while (true) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(LOAD_SQL, lastUserId, warmupBatchSize);

                if (rows.isEmpty()) {
                    break;
                }

                for (Map<String, Object> row : rows) {
                    Object email = row.get("email");
                    if (email != null) {
                        bloomFilter.put(email.toString().trim().toLowerCase());
                    }

                    Number userId = (Number) row.get("user_id");
                    if (userId != null) {
                        lastUserId = userId.longValue();
                    }
                }

                loaded += rows.size();
            }

            warmedUp.set(true);
            log.info(
                "AuthEmailBloomFilter warm-up completed. loaded={}, elapsedMs={}",
                loaded,
                System.currentTimeMillis() - startedAt
            );
        } catch (Exception exception) {
            warmedUp.set(false);
            log.warn(
                "AuthEmailBloomFilter warm-up failed. Fallback to DB check only. elapsedMs={}",
                System.currentTimeMillis() - startedAt,
                exception
            );
        }
    }

    public boolean shouldCheckDb(String normalizedEmail) {
        if (!enabled || !warmedUp.get()) {
            return true;
        }

        return bloomFilter.mightContain(normalizedEmail);
    }

    public void put(String normalizedEmail) {
        if (!enabled) {
            return;
        }

        bloomFilter.put(normalizedEmail);
    }
}
