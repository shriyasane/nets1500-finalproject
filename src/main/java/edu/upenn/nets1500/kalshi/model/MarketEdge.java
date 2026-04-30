package edu.upenn.nets1500.kalshi.model;

public record MarketEdge(String sourceTicker, String targetTicker, double similarityScore) {
    public MarketEdge {
        sourceTicker = requireNonBlank(sourceTicker, "sourceTicker");
        targetTicker = requireNonBlank(targetTicker, "targetTicker");

        if (similarityScore < 0.0 || similarityScore > 1.0) {
            throw new IllegalArgumentException("similarityScore must be between 0.0 and 1.0");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
