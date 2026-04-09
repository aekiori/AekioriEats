package com.delivery.auth.service;

import com.delivery.auth.config.AuthRateLimitProperties;
import com.delivery.auth.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimitServiceTest {

    @Test
    void validate_login_blocks_when_account_limit_exceeded() {
        AuthRateLimitService rateLimitService = new AuthRateLimitService(
            properties(true, 20, 60, 20, 300, 20, 60, 2, 300, 1000)
        );

        rateLimitService.validateLogin("user@example.com", "127.0.0.1");
        rateLimitService.validateLogin("user@example.com", "127.0.0.1");

        assertThatThrownBy(() -> rateLimitService.validateLogin("user@example.com", "127.0.0.1"))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException exception = (ApiException) error;
                assertThat(exception.getCode()).isEqualTo("AUTH_RATE_LIMITED");
                assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            });
    }

    @Test
    void clear_login_account_limit_resets_account_counter() {
        AuthRateLimitService rateLimitService = new AuthRateLimitService(
            properties(true, 20, 60, 20, 300, 20, 60, 1, 300, 1000)
        );

        rateLimitService.validateLogin("user@example.com", "127.0.0.1");
        rateLimitService.clearLoginAccountLimit("user@example.com");

        assertThatCode(() -> rateLimitService.validateLogin("user@example.com", "127.0.0.1"))
            .doesNotThrowAnyException();
    }

    @Test
    void disabled_mode_never_blocks() {
        AuthRateLimitService rateLimitService = new AuthRateLimitService(
            properties(false, 1, 60, 1, 300, 1, 60, 1, 300, 1000)
        );

        assertThatCode(() -> {
            rateLimitService.validateSignup("user@example.com", "127.0.0.1");
            rateLimitService.validateSignup("user@example.com", "127.0.0.1");
            rateLimitService.validateLogin("user@example.com", "127.0.0.1");
            rateLimitService.validateLogin("user@example.com", "127.0.0.1");
        }).doesNotThrowAnyException();
    }

    private AuthRateLimitProperties properties(
        boolean enabled,
        int signupIpMaxRequests,
        long signupIpWindowSeconds,
        int signupAccountMaxRequests,
        long signupAccountWindowSeconds,
        int loginIpMaxRequests,
        long loginIpWindowSeconds,
        int loginAccountMaxRequests,
        long loginAccountWindowSeconds,
        int cleanupThreshold
    ) {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.setEnabled(enabled);
        properties.setCleanupThreshold(cleanupThreshold);
        properties.getSignup().getIp().setRequestLimit(signupIpMaxRequests);
        properties.getSignup().getIp().setRateLimitPeriodSeconds(signupIpWindowSeconds);
        properties.getSignup().getAccount().setRequestLimit(signupAccountMaxRequests);
        properties.getSignup().getAccount().setRateLimitPeriodSeconds(signupAccountWindowSeconds);
        properties.getLogin().getIp().setRequestLimit(loginIpMaxRequests);
        properties.getLogin().getIp().setRateLimitPeriodSeconds(loginIpWindowSeconds);
        properties.getLogin().getAccount().setRequestLimit(loginAccountMaxRequests);
        properties.getLogin().getAccount().setRateLimitPeriodSeconds(loginAccountWindowSeconds);
        return properties;
    }
}
