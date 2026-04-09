package com.delivery.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.rate-limit")
public class AuthRateLimitProperties {
    private boolean enabled = true;
    private int cleanupThreshold = 100000;
    private Endpoint signup = Endpoint.signupDefaults();
    private Endpoint login = Endpoint.loginDefaults();

    public int cleanupThreshold() {
        return Math.max(1000, cleanupThreshold);
    }

    public long maxWindowSeconds() {
        return Math.max(
            Math.max(signup.getIp().rateLimitPeriodSeconds(), signup.getAccount().rateLimitPeriodSeconds()),
            Math.max(login.getIp().rateLimitPeriodSeconds(), login.getAccount().rateLimitPeriodSeconds())
        );
    }

    @Getter
    @Setter
    public static class Endpoint {
        private Limit ip = new Limit();
        private Limit account = new Limit();

        static Endpoint signupDefaults() {
            Endpoint endpoint = new Endpoint();
            endpoint.ip.requestLimit = 20;
            endpoint.ip.rateLimitPeriodSeconds = 60;
            endpoint.account.requestLimit = 5;
            endpoint.account.rateLimitPeriodSeconds = 300;
            return endpoint;
        }

        static Endpoint loginDefaults() {
            Endpoint endpoint = new Endpoint();
            endpoint.ip.requestLimit = 60;
            endpoint.ip.rateLimitPeriodSeconds = 60;
            endpoint.account.requestLimit = 10;
            endpoint.account.rateLimitPeriodSeconds = 300;
            return endpoint;
        }

    }

    @Getter
    @Setter
    public static class Limit {
        private int requestLimit = 1;
        private long rateLimitPeriodSeconds = 60;

        public int requestLimit() {
            return Math.max(1, requestLimit);
        }

        public long rateLimitPeriodSeconds() {
            return Math.max(1, rateLimitPeriodSeconds);
        }
    }
}
