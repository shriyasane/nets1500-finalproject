package edu.upenn.nets1500.kalshi.model;

public enum MarketStatus {
    INITIALIZED,
    UNOPENED,
    OPEN,
    PAUSED,
    CLOSED,
    SETTLED,
    SUSPENDED,
    UNKNOWN

    ;

    public static MarketStatus fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        return switch (value.trim().toLowerCase()) {
            case "initialized" -> INITIALIZED;
            case "unopened" -> UNOPENED;
            case "open" -> OPEN;
            case "paused" -> PAUSED;
            case "closed" -> CLOSED;
            case "settled" -> SETTLED;
            case "suspended" -> SUSPENDED;
            default -> UNKNOWN;
        };
    }
}
