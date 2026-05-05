package edu.upenn.nets1500.kalshi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.io.IOException;
import java.util.List;
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

    private static final class FakeKalshiApiClient implements KalshiApiClient {
        private final String payload;

        private FakeKalshiApiClient(String payload) {
            this.payload = payload;
        }

        @Override
        public String getMarkets(int limit) {
            return payload;
        }
    }
}
