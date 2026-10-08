package com.delivery.gateway.filter;

import com.delivery.gateway.config.GatewayRateLimitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitFilterTest {
    private GatewayRateLimitProperties properties;
    private GatewayRateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new GatewayRateLimitProperties();
        properties.getAuth().setRequestLimit(1);
        properties.getAuth().setRateLimitPeriodSeconds(60);
        properties.getPublicApi().setRequestLimit(1);
        properties.getPublicApi().setRateLimitPeriodSeconds(60);
        objectMapper = new ObjectMapper();
        filter = new GatewayRateLimitFilter(properties, objectMapper);
    }

    @Test
    void filter_blocks_auth_path_when_ip_limit_is_exceeded() throws Exception {
        CountingChain chain = new CountingChain();

        StepVerifier.create(filter.filter(exchange("/api/v1/auth/login", "10.0.0.1"), chain))
            .verifyComplete();

        MockServerWebExchange blockedExchange = exchange(
            "/api/v1/auth/login",
            "10.0.0.1",
            "trace-rate-001"
        );
        StepVerifier.create(filter.filter(blockedExchange, chain))
            .verifyComplete();

        assertThat(chain.invocationCount.get()).isEqualTo(1);
        assertThat(blockedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        JsonNode body = objectMapper.readTree(blockedExchange.getResponse().getBodyAsString().block());
        assertThat(body.path("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-rate-001");
    }

    @Test
    void filter_keeps_separate_buckets_per_client_ip() {
        CountingChain chain = new CountingChain();

        StepVerifier.create(filter.filter(exchange("/api/v1/stores/search", "10.0.0.1"), chain))
            .verifyComplete();
        StepVerifier.create(filter.filter(exchange("/api/v1/stores/search", "10.0.0.2"), chain))
            .verifyComplete();

        assertThat(chain.invocationCount.get()).isEqualTo(2);
    }

    @Test
    void filter_skips_unconfigured_paths() {
        CountingChain chain = new CountingChain();

        StepVerifier.create(filter.filter(exchange("/api/v1/orders/1", "10.0.0.1"), chain))
            .verifyComplete();
        StepVerifier.create(filter.filter(exchange("/api/v1/orders/1", "10.0.0.1"), chain))
            .verifyComplete();

        assertThat(chain.invocationCount.get()).isEqualTo(2);
    }

    @Test
    void disabled_filter_never_blocks() {
        properties.setEnabled(false);
        CountingChain chain = new CountingChain();

        StepVerifier.create(filter.filter(exchange("/api/v1/auth/login", "10.0.0.1"), chain))
            .verifyComplete();
        StepVerifier.create(filter.filter(exchange("/api/v1/auth/login", "10.0.0.1"), chain))
            .verifyComplete();

        assertThat(chain.invocationCount.get()).isEqualTo(2);
    }

    private MockServerWebExchange exchange(String path, String clientIp) {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get(path)
                .remoteAddress(new InetSocketAddress(clientIp, 12345))
                .build()
        );
    }

    private MockServerWebExchange exchange(String path, String clientIp, String traceId) {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get(path)
                .header(GatewayTraceIdFilter.TRACE_ID_HEADER, traceId)
                .remoteAddress(new InetSocketAddress(clientIp, 12345))
                .build()
        );
    }

    private static class CountingChain implements GatewayFilterChain {
        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            invocationCount.incrementAndGet();
            return Mono.empty();
        }
    }
}
