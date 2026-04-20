package com.delivery.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/gateway/test/status")
public class GatewayTestController {

    @GetMapping("/400")
    public Mono<ResponseEntity<Map<String, String>>> forceBadRequest() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                    "code", "GATEWAY_TEST_BAD_REQUEST",
                    "message", "Intentional bad request for metrics test."
                )
            )
        );
    }

    @GetMapping("/500")
    public Mono<Void> forceInternalServerError() {
        return Mono.error(new IllegalStateException("Intentional internal server error for metrics test."));
    }

    @GetMapping("/latency")
    public Mono<ResponseEntity<Map<String, Object>>> randomLatency() {
        long delayMillis = randomDelayMillis();

        return Mono.delay(Duration.ofMillis(delayMillis))
            .map(ignored -> ResponseEntity.ok(
                Map.of(
                    "domain", "gateway",
                    "status", "OK",
                    "delayMs", delayMillis
                )
            ));
    }

    @GetMapping("/chaos")
    public Mono<ResponseEntity<Map<String, Object>>> causeChaos() {
        long delayMillis = chaosDelayMillis();
        int errorChance = ThreadLocalRandom.current().nextInt(100);

        return Mono.delay(Duration.ofMillis(delayMillis))
            .map(ignored -> {
                if (errorChance < 5) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of(
                            "domain", "gateway",
                            "status", "ERROR_500",
                            "delayMs", delayMillis
                        )
                    );
                }

                if (errorChance < 10) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of(
                            "domain", "gateway",
                            "status", "ERROR_400",
                            "delayMs", delayMillis
                        )
                    );
                }

                return ResponseEntity.ok(
                    Map.of(
                        "domain", "gateway",
                        "status", "OK",
                        "delayMs", delayMillis
                    )
                );
            });
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
}
