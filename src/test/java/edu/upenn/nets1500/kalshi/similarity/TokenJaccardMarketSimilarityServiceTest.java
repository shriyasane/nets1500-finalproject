package edu.upenn.nets1500.kalshi.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenJaccardMarketSimilarityServiceTest {
    private final TokenJaccardMarketSimilarityService similarityService =
            new TokenJaccardMarketSimilarityService();

    @Test
    void returnsOneForSameMarketTicker() {
        Market market = market(
                "FED-SEP-CUT",
                "Will the Fed cut rates in September?",
                "Federal Reserve policy meeting");

        assertEquals(1.0, similarityService.score(market, market));
    }

    @Test
    void scoresRelatedMarketsHigherThanUnrelatedMarkets() {
        Market fedSeptember = market(
                "FED-SEP-CUT",
                "Will the Fed cut rates in September?",
                "Federal Reserve policy meeting");
        Market fedDecember = market(
                "FED-DEC-CUT",
                "Will the Fed cut rates in December?",
                "Federal Reserve policy decision");
        Market weather = market(
                "TEMP-NYC-90",
                "Will New York reach 90 degrees?",
                "Daily high temperature in NYC");

        double relatedScore = similarityService.score(fedSeptember, fedDecember);
        double unrelatedScore = similarityService.score(fedSeptember, weather);

        assertTrue(relatedScore > unrelatedScore);
        assertTrue(relatedScore > 0.0);
    }

    @Test
    void ignoresStopWordsAndPunctuationDuringTokenization() {
        Market market = market(
                "FED-SEP-CUT",
                "Will the Fed cut rates in September?",
                "The Federal Reserve, in September.");

        Set<String> tokens = similarityService.extractKeywords(market);

        assertTrue(tokens.contains("fed"));
        assertTrue(tokens.contains("cut"));
        assertTrue(tokens.contains("rates"));
        assertTrue(tokens.contains("september"));
        assertTrue(tokens.contains("federal"));
        assertTrue(tokens.contains("reserve"));
        assertTrue(!tokens.contains("the"));
        assertTrue(!tokens.contains("will"));
        assertTrue(!tokens.contains("in"));
    }

    @Test
    void returnsZeroWhenMarketsShareNoKeywords() {
        Market inflation = market(
                "CPI-HIGH",
                "Will CPI exceed 4 percent?",
                "Consumer inflation reading");
        Market sports = market(
                "NBA-KNICKS",
                "Will the Knicks win tonight?",
                "Basketball game outcome");

        assertEquals(0.0, similarityService.score(inflation, sports));
    }

    private static Market market(String ticker, String title, String description) {
        return new Market(
                ticker,
                title,
                null,
                description,
                null,
                null,
                null,
                MarketStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
