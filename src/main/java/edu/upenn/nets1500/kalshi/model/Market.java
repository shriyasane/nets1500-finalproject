package edu.upenn.nets1500.kalshi.model;

import java.time.Instant;
import java.util.Objects;

public record Market(
        String ticker,
        String title,
        String subtitle,
        String description,
        String category,
        String seriesTicker,
        String eventTicker,
        MarketStatus status,
        Instant openTime,
        Instant closeTime,
        Instant expirationTime,
        Double yesAskPrice,
        Double noAskPrice,
        Double lastTradedPrice) {

    public Market {
        ticker = requireNonBlank(ticker, "ticker");
        title = requireNonBlank(title, "title");
        subtitle = normalizeText(subtitle);
        description = normalizeText(description);
        category = normalizeText(category);
        seriesTicker = normalizeText(seriesTicker);
        eventTicker = normalizeText(eventTicker);
        status = Objects.requireNonNullElse(status, MarketStatus.UNKNOWN);

        validateChronology(openTime, closeTime, expirationTime);
        validateProbability(yesAskPrice, "yesAskPrice");
        validateProbability(noAskPrice, "noAskPrice");
        validateProbability(lastTradedPrice, "lastTradedPrice");
    }

    public boolean hasPriceData() {
        return yesAskPrice != null || noAskPrice != null || lastTradedPrice != null;
    }

    public boolean hasSchedule() {
        return openTime != null || closeTime != null || expirationTime != null;
    }

    public String similarityText() {
        return joinText(title, subtitle, description, category);
    }

    private static void validateChronology(Instant openTime, Instant closeTime, Instant expirationTime) {
        if (openTime != null && closeTime != null && openTime.isAfter(closeTime)) {
            throw new IllegalArgumentException("openTime cannot be after closeTime");
        }

        if (closeTime != null && expirationTime != null && closeTime.isAfter(expirationTime)) {
            throw new IllegalArgumentException("closeTime cannot be after expirationTime");
        }
    }

    private static void validateProbability(Double value, String fieldName) {
        if (value != null && (value < 0.0 || value > 1.0)) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String joinText(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
