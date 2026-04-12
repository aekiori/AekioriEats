package com.delivery.store.controller.store;

import com.delivery.store.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/stores/test/status")
public class StoreTestController {

    @GetMapping("/400")
    public void forceBadRequest() {
        throw new ApiException(
            "STORE_TEST_BAD_REQUEST",
            "Intentional bad request for metrics test.",
            HttpStatus.BAD_REQUEST
        );
    }

    @GetMapping("/500")
    public void forceInternalServerError() {
        throw new IllegalStateException("Intentional internal server error for metrics test.");
    }

    @GetMapping("/latency")
    public ResponseEntity<Map<String, Object>> randomLatency() {
        long delayMillis = randomDelayMillis();
        sleep(delayMillis);

        return ResponseEntity.ok(
            Map.of(
                "domain", "store",
                "status", "OK",
                "delayMs", delayMillis
            )
        );
    }

    @GetMapping("/chaos")
    public ResponseEntity<Map<String, Object>> causeChaos() {
        long delayMillis = chaosDelayMillis();
        sleep(delayMillis);

        int errorChance = ThreadLocalRandom.current().nextInt(100);

        if (errorChance < 5) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                    "domain", "store",
                    "status", "ERROR_500",
                    "delayMs", delayMillis
                )
            );
        }

        if (errorChance < 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                    "domain", "store",
                    "status", "ERROR_400",
                    "delayMs", delayMillis
                )
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "domain", "store",
                "status", "OK",
                "delayMs", delayMillis
            )
        );
    }

    private long randomDelayMillis() {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 70) {
            return 40L;
        }
        if (roll < 90) {
            return 120L;
        }
        if (roll < 98) {
            return 350L;
        }
        return 900L;
    }

    private long chaosDelayMillis() {
        int chance = ThreadLocalRandom.current().nextInt(100);

        if (chance < 80) {
            return ThreadLocalRandom.current().nextLong(50L);
        }
        if (chance < 95) {
            return 200L + ThreadLocalRandom.current().nextLong(300L);
        }
        return 1000L + ThreadLocalRandom.current().nextLong(2000L);
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during test latency sleep.", exception);
        }
    }
}
