package com.delivery.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {
    private boolean enabled = true;
    private boolean trustForwardedFor = false;
    private int cleanupThreshold = 100000;
    private Route auth = Route.withDefaults(List.of("/api/v1/auth/**"), 60, 60);
    private Route publicApi = Route.withDefaults(List.of("/api/v1/stores/**"), 300, 60);

    public int cleanupThreshold() {
        return Math.max(1000, cleanupThreshold);
    }

    public long maxWindowSeconds() {
        return Math.max(auth.rateLimitPeriodSeconds(), publicApi.rateLimitPeriodSeconds());
    }

    public List<Route> enabledRoutes() {
        return List.of(auth, publicApi).stream()
            .filter(Route::isEnabled)
            .filter(route -> !route.getPathPatterns().isEmpty())
            .toList();
    }

    @Getter
    @Setter
    public static class Route {
        private boolean enabled = true;
        private List<String> pathPatterns = new ArrayList<>();
        private int requestLimit = 1;
        private long rateLimitPeriodSeconds = 60;

        static Route withDefaults(List<String> pathPatterns, int requestLimit, long rateLimitPeriodSeconds) {
            Route route = new Route();
            route.pathPatterns = new ArrayList<>(pathPatterns);
            route.requestLimit = requestLimit;
            route.rateLimitPeriodSeconds = rateLimitPeriodSeconds;
            return route;
        }

        public int requestLimit() {
            return Math.max(1, requestLimit);
        }

        public long rateLimitPeriodSeconds() {
            return Math.max(1, rateLimitPeriodSeconds);
        }
    }
}
