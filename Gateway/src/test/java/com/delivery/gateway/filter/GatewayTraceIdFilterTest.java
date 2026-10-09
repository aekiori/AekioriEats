package com.delivery.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GatewayTraceIdFilterTest {
    private final GatewayTraceIdFilter filter = new GatewayTraceIdFilter();

    @Test
    void filter_generates_trace_id_when_header_is_missing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1").build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        ServerWebExchange tracedExchange = chain.exchangeRef.get();
        String traceId = tracedExchange.getRequest().getHeaders().getFirst(GatewayTraceIdFilter.TRACE_ID_HEADER);
        String attributeTraceId = tracedExchange.getAttribute(GatewayTraceIdFilter.TRACE_ID_ATTRIBUTE);

        assertThat(traceId).isNotBlank();
        assertThatCode(() -> UUID.fromString(traceId)).doesNotThrowAnyException();
        assertThat(attributeTraceId).isEqualTo(traceId);
        assertThat(exchange.getResponse().getHeaders().getFirst(GatewayTraceIdFilter.TRACE_ID_HEADER))
            .isEqualTo(traceId);
    }

    @Test
    void filter_preserves_valid_incoming_trace_id() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1")
                .header(GatewayTraceIdFilter.TRACE_ID_HEADER, " trace-001 ")
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        ServerWebExchange tracedExchange = chain.exchangeRef.get();
        String attributeTraceId = tracedExchange.getAttribute(GatewayTraceIdFilter.TRACE_ID_ATTRIBUTE);

        assertThat(tracedExchange.getRequest().getHeaders().getFirst(GatewayTraceIdFilter.TRACE_ID_HEADER))
            .isEqualTo("trace-001");
        assertThat(attributeTraceId).isEqualTo("trace-001");
        assertThat(exchange.getResponse().getHeaders().getFirst(GatewayTraceIdFilter.TRACE_ID_HEADER))
            .isEqualTo("trace-001");
    }

    @Test
    void filter_replaces_invalid_incoming_trace_id() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1")
                .header(GatewayTraceIdFilter.TRACE_ID_HEADER, "bad trace")
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        String traceId = chain.exchangeRef.get()
            .getRequest()
            .getHeaders()
            .getFirst(GatewayTraceIdFilter.TRACE_ID_HEADER);

        assertThat(traceId).isNotEqualTo("bad trace");
        assertThatCode(() -> UUID.fromString(traceId)).doesNotThrowAnyException();
    }

    @Test
    void filter_runs_before_rate_limit_and_authentication_filters() {
        assertThat(filter.getOrder()).isEqualTo(-300);
    }

    private static class CapturingChain implements GatewayFilterChain {
        private final AtomicReference<ServerWebExchange> exchangeRef = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            exchangeRef.set(exchange);
            return Mono.empty();
        }
    }
}
