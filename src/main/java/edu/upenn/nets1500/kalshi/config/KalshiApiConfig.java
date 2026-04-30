package edu.upenn.nets1500.kalshi.config;

import java.time.Duration;

public record KalshiApiConfig(String baseUrl, Duration requestTimeout, int pageSize) {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_PAGE_SIZE = 100;

    public KalshiApiConfig {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        baseUrl = baseUrl.trim();
        requestTimeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;

        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
    }

    public KalshiApiConfig(String baseUrl) {
        this(baseUrl, DEFAULT_TIMEOUT, DEFAULT_PAGE_SIZE);
    }
}
