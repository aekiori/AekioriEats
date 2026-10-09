package com.delivery.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class GatewayTraceIdFilter implements GlobalFilter, Ordered {
    public static final String TRACE_ID_ATTRIBUTE = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final int MAX_TRACE_ID_LENGTH = 128;
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange);

        ServerHttpRequest request = exchange.getRequest().mutate()
            .headers(headers -> headers.set(TRACE_ID_HEADER, traceId))
            .build();

        ServerWebExchange tracedExchange = exchange.mutate()
            .request(request)
            .build();

        tracedExchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);
        tracedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        return chain.filter(tracedExchange);
    }

    @Override
    public int getOrder() {
        return -300;
    }

    public static String getTraceId(ServerWebExchange exchange) {
        Object traceId = exchange.getAttribute(TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }

        String headerTraceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (isValidTraceId(headerTraceId)) {
            return headerTraceId.trim();
        }

        return null;
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (isValidTraceId(traceId)) {
            return traceId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private static boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return false;
        }

        String normalizedTraceId = traceId.trim();
        return normalizedTraceId.length() <= MAX_TRACE_ID_LENGTH
            && TRACE_ID_PATTERN.matcher(normalizedTraceId).matches();
    }
}
