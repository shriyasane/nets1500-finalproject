package edu.upenn.nets1500.kalshi;

import edu.upenn.nets1500.kalshi.api.HttpKalshiApiClient;
import edu.upenn.nets1500.kalshi.api.KalshiMarketFetcher;
import edu.upenn.nets1500.kalshi.config.KalshiApiConfig;
import edu.upenn.nets1500.kalshi.graph.GraphBuilder;
import edu.upenn.nets1500.kalshi.graph.MarketGraph;
import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import edu.upenn.nets1500.kalshi.similarity.TokenJaccardMarketSimilarityService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;

public final class App {
    private static final int DEFAULT_FETCH_LIMIT = 15;

    private App() {
    }

    public static void main(String[] args) {
        KalshiApiConfig config = new KalshiApiConfig("https://api.elections.kalshi.com/trade-api/v2");
        HttpClient httpClient = HttpClient.newHttpClient();
        KalshiMarketFetcher fetcher = new KalshiMarketFetcher(new HttpKalshiApiClient(httpClient, config));
        GraphBuilder graphBuilder = new GraphBuilder(new TokenJaccardMarketSimilarityService());

        try {
            List<Market> markets = fetcher.fetchMarkets(DEFAULT_FETCH_LIMIT);
            if (markets.isEmpty()) {
                System.out.println("No markets returned from Kalshi.");
                return;
            }

            MarketGraph graph = graphBuilder.buildGraph(markets);
            System.out.printf(
                    "Fetched %d markets and built a graph with %d edges.%n",
                    graph.marketCount(),
                    graph.edgeCount());
            printNeighborPreview(graph);
        } catch (IOException e) {
            System.err.println("Failed to fetch Kalshi markets: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Kalshi fetch was interrupted.");
        }
    }

    private static void printNeighborPreview(MarketGraph graph) {
        System.out.println("Top neighbors by market:");
        for (Market market : graph.markets()) {
            List<MarketEdge> neighbors = graph.neighborsOf(market.ticker());
            if (neighbors.isEmpty()) {
                System.out.printf("%s -> no similar neighbors above threshold%n", market.ticker());
                continue;
            }

            StringBuilder neighborSummary = new StringBuilder();
            for (int i = 0; i < neighbors.size(); i++) {
                MarketEdge edge = neighbors.get(i);
                if (i > 0) {
                    neighborSummary.append(", ");
                }
                neighborSummary.append(graph.otherEndpoint(market.ticker(), edge))
                        .append(" (")
                        .append(String.format("%.2f", edge.similarityScore()))
                        .append(")");
            }

            System.out.printf("%s -> %s%n", market.ticker(), neighborSummary);
        }
    }
}
