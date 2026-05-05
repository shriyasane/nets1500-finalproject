package edu.upenn.nets1500.kalshi;

import edu.upenn.nets1500.kalshi.api.HttpKalshiApiClient;
import edu.upenn.nets1500.kalshi.api.KalshiMarketFetcher;
import edu.upenn.nets1500.kalshi.config.KalshiApiConfig;
import edu.upenn.nets1500.kalshi.model.Market;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        KalshiApiConfig config = new KalshiApiConfig("https://api.elections.kalshi.com/trade-api/v2");
        HttpClient httpClient = HttpClient.newHttpClient();
        KalshiMarketFetcher fetcher = new KalshiMarketFetcher(new HttpKalshiApiClient(httpClient, config));

        try {
            List<Market> markets = fetcher.fetchMarkets(5);
            if (markets.isEmpty()) {
                System.out.println("No markets returned from Kalshi.");
                return;
            }

            for (Market market : markets) {
                System.out.printf(
                        "%s | %s | status=%s | close=%s%n",
                        market.ticker(),
                        market.title(),
                        market.status(),
                        market.closeTime());
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch Kalshi markets: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Kalshi fetch was interrupted.");
        }
    }
}
