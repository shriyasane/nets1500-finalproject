package edu.upenn.nets1500.kalshi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KalshiMarketFetcherTest {
    @Test
    void mapsKalshiMarketResponseIntoDomainMarkets() throws IOException, InterruptedException {
        String payload = """
                {
                  "markets": [
                    {
                      "ticker": "KXFEDRATE-26MAY-TN",
                      "event_ticker": "KXFEDRATE-26MAY",
                      "status": "open",
                      "open_time": "2026-05-01T12:00:00Z",
                      "close_time": "2026-05-08T12:00:00Z",
                      "expiration_time": "2026-05-08T12:05:00Z",
                      "yes_ask_dollars": "0.4300",
                      "no_ask_dollars": "0.5900",
                      "last_price_dollars": "0.4400",
                      "title": "Will the Fed cut rates in May?",
                      "subtitle": "Fed decision",
                      "rules_primary": "Resolves to the FOMC statement."
                    },
                    {
                      "ticker": "KXINF-26JUN-HIGH",
                      "event_ticker": "KXINF-26JUN",
                      "status": "settled",
                      "open_time": null,
                      "close_time": null,
                      "expiration_time": null,
                      "yes_ask_dollars": null,
                      "no_ask_dollars": null,
                      "last_price_dollars": null,
                      "title": "Will CPI print above 3%?",
                      "subtitle": null,
                      "rules_primary": null
                    }
                  ],
                  "cursor": ""
                }
                """;

        KalshiApiClient fakeClient = new FakeKalshiApiClient(payload);
        KalshiMarketFetcher fetcher = new KalshiMarketFetcher(fakeClient);

        List<Market> markets = fetcher.fetchMarkets(2);

        assertEquals(2, markets.size());

        Market first = markets.get(0);
        assertEquals("KXFEDRATE-26MAY-TN", first.ticker());
        assertEquals("KXFEDRATE-26MAY", first.eventTicker());
        assertEquals(MarketStatus.OPEN, first.status());
        assertEquals(0.43, first.yesAskPrice());
        assertEquals("Resolves to the FOMC statement.", first.description());

        Market second = markets.get(1);
        assertEquals("KXINF-26JUN-HIGH", second.ticker());
        assertEquals(MarketStatus.SETTLED, second.status());
    }

    @Test
    void fetchesDiversifiedMarketsAcrossSeriesCategories() throws IOException, InterruptedException {
        String seriesPayload = """
                {
                  "series": [
                    { "ticker": "SPORTS-SERIES", "category": "Sports" },
                    { "ticker": "ENT-SERIES", "category": "Entertainment" },
                    { "ticker": "POL-SERIES", "category": "Politics" }
                  ]
                }
                """;

        Map<String, String> marketsBySeries = new LinkedHashMap<>();
        marketsBySeries.put("SPORTS-SERIES", marketsPayload(
                marketJson("SPORTS-1", "Will the Knicks win tonight?", "SPORTS-EVENT-1"),
                marketJson("SPORTS-2", "Will the Mets win tonight?", "SPORTS-EVENT-2")));
        marketsBySeries.put("ENT-SERIES", marketsPayload(
                marketJson("ENT-1", "Will this movie top the box office?", "ENT-EVENT-1"),
                marketJson("ENT-2", "Will this song hit number one?", "ENT-EVENT-2")));
        marketsBySeries.put("POL-SERIES", marketsPayload(
                marketJson("POL-1", "Will candidate X win the debate?", "POL-EVENT-1"),
                marketJson("POL-2", "Will the bill pass the Senate?", "POL-EVENT-2")));

        KalshiApiClient fakeClient = new FakeKalshiApiClient(marketsPayload(), seriesPayload, marketsBySeries);
        KalshiMarketFetcher fetcher = new KalshiMarketFetcher(fakeClient);

        List<Market> markets = fetcher.fetchDiversifiedMarkets(5);

        assertEquals(5, markets.size());
        assertTrue(markets.stream().anyMatch(market -> market.ticker().startsWith("SPORTS-")));
        assertTrue(markets.stream().anyMatch(market -> market.ticker().startsWith("ENT-")));
        assertTrue(markets.stream().anyMatch(market -> market.ticker().startsWith("POL-")));
    }

    private static final class FakeKalshiApiClient implements KalshiApiClient {
        private final String defaultMarketsPayload;
        private final String seriesPayload;
        private final Map<String, String> marketsBySeries;

        private FakeKalshiApiClient(String defaultMarketsPayload) {
            this(defaultMarketsPayload, "{\"series\":[]}", Map.of());
        }

        private FakeKalshiApiClient(
                String defaultMarketsPayload,
                String seriesPayload,
                Map<String, String> marketsBySeries) {
            this.defaultMarketsPayload = defaultMarketsPayload;
            this.seriesPayload = seriesPayload;
            this.marketsBySeries = marketsBySeries;
        }

        @Override
        public String getMarkets(int limit) {
            return defaultMarketsPayload;
        }

        @Override
        public String getMarkets(int limit, String seriesTicker, String status) {
            return marketsBySeries.getOrDefault(seriesTicker, defaultMarketsPayload);
        }

        @Override
        public String getSeries(String category) {
            return seriesPayload;
        }
    }

    private static String marketsPayload(String... marketObjects) {
        return """
                {
                  "markets": [
                """
                + String.join(",", marketObjects)
                + """
                  ]
                }
                """;
    }

    private static String marketJson(String ticker, String title, String eventTicker) {
        return """
                {
                  "ticker": "%s",
                  "event_ticker": "%s",
                  "status": "open",
                  "open_time": null,
                  "close_time": null,
                  "expiration_time": null,
                  "yes_ask_dollars": "0.4300",
                  "no_ask_dollars": "0.5900",
                  "last_price_dollars": "0.4400",
                  "title": "%s",
                  "subtitle": null,
                  "rules_primary": null
                }
                """.formatted(ticker, eventTicker, title);
    }
}
