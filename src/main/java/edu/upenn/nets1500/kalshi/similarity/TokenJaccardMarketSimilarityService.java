package edu.upenn.nets1500.kalshi.similarity;

import edu.upenn.nets1500.kalshi.model.Market;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class TokenJaccardMarketSimilarityService implements MarketSimilarityService {
    private static final Set<String> STOP_WORDS = Set.of(
            "a",
            "an",
            "and",
            "are",
            "as",
            "at",
            "be",
            "by",
            "for",
            "from",
            "in",
            "is",
            "of",
            "on",
            "or",
            "that",
            "the",
            "to",
            "was",
            "will",
            "with");

    @Override
    public double score(Market first, Market second) {
        Objects.requireNonNull(first, "first market must not be null");
        Objects.requireNonNull(second, "second market must not be null");

        if (first.ticker().equals(second.ticker())) {
            return 1.0;
        }

        Set<String> firstTokens = extractKeywords(first);
        Set<String> secondTokens = extractKeywords(second);
        if (firstTokens.isEmpty() && secondTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new LinkedHashSet<>(firstTokens);
        intersection.retainAll(secondTokens);

        Set<String> union = new LinkedHashSet<>(firstTokens);
        union.addAll(secondTokens);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    Set<String> extractKeywords(Market market) {
        return tokenize(market.similarityText());
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .filter(token -> !STOP_WORDS.contains(token))
                .forEach(tokens::add);
        return tokens;
    }
}
