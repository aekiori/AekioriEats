package com.delivery.order.exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {
    private static final String INTERNAL_API_PREFIX = "/api/v1/internal/";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Value("${order.internal.api-key}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String providedApiKey = request.getHeader(INTERNAL_API_KEY_HEADER);

        if (providedApiKey == null || !providedApiKey.equals(internalApiKey)) {
            handlerExceptionResolver.resolveException(request, response, null, new ApiException(
                "INTERNAL_API_UNAUTHORIZED",
                "Internal API authentication failed.",
                HttpStatus.UNAUTHORIZED
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
