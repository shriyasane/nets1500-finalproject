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
            printNearestNeighborDemo(graph, markets.get(0).ticker(), 3);
            printNeighborPreview(graph);
        } catch (IOException e) {
            System.err.println("Failed to fetch Kalshi markets: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Kalshi fetch was interrupted.");
        }
    }

    private static void printNearestNeighborDemo(MarketGraph graph, String ticker, int limit) {
        Market market = graph.market(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + ticker));
        System.out.printf("Nearest neighbors for %s:%n", formatMarketLabel(market));
        List<MarketGraph.Neighbor> neighbors = graph.nearestNeighborsOf(ticker, limit);
        if (neighbors.isEmpty()) {
            System.out.println("No similar neighbors above threshold.");
            return;
        }

        for (MarketGraph.Neighbor neighbor : neighbors) {
            System.out.printf(
                    "%s -> %.2f%n",
                    formatMarketLabel(neighbor.market()),
                    neighbor.similarityScore());
        }
    }

    private static void printNeighborPreview(MarketGraph graph) {
        System.out.println("Top neighbors by market:");
        for (Market market : graph.markets()) {
            System.out.println();
            System.out.println(formatMarketLabel(market));
            List<MarketEdge> neighbors = graph.neighborsOf(market.ticker());
            if (neighbors.isEmpty()) {
                System.out.println("  No similar neighbors above threshold.");
                continue;
            }

            for (int i = 0; i < neighbors.size(); i++) {
                MarketEdge edge = neighbors.get(i);
                Market neighbor = graph.market(graph.otherEndpoint(market.ticker(), edge))
                        .orElseThrow(() -> new IllegalStateException("Missing market for graph edge"));
                System.out.printf(
                        "  %d. %s%n",
                        i + 1,
                        formatMarketLabel(neighbor));
                System.out.printf(
                        "     score: %.2f%n",
                        edge.similarityScore());
            }
        }
    }

    private static String formatMarketLabel(Market market) {
        return "%s | %s [%s]".formatted(
                buildShortTitle(market),
                market.title(),
                abbreviateTicker(market.ticker()));
    }

    private static String buildShortTitle(Market market) {
        String title = market.title();
        String[] clauses = title.split("\\s*,\\s*");

        if (looksLikeLegList(clauses)) {
            return buildParlayLabel(clauses);
        }

        if (clauses.length == 1) {
            return truncate(clauses[0], 48);
        }

        int clauseLimit = Math.min(3, clauses.length);
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < clauseLimit; i++) {
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(clauses[i].trim());
        }

        if (clauses.length > clauseLimit) {
            summary.append(", ...");
        }

        return truncate(summary.toString(), 64);
    }

    private static boolean looksLikeLegList(String[] clauses) {
        if (clauses.length < 2) {
            return false;
        }

        int prefixedClauses = 0;
        for (String clause : clauses) {
            String normalizedClause = clause.trim().toLowerCase();
            if (normalizedClause.startsWith("yes ") || normalizedClause.startsWith("no ")) {
                prefixedClauses++;
            }
        }

        return prefixedClauses >= Math.max(2, clauses.length / 2);
    }

    private static String buildParlayLabel(String[] clauses) {
        int clauseLimit = Math.min(3, clauses.length);
        StringBuilder summary = new StringBuilder("All-leg parlay: ");

        for (int i = 0; i < clauseLimit; i++) {
            if (i > 0) {
                summary.append("; ");
            }
            summary.append(cleanClause(clauses[i]));
        }

        if (clauses.length > clauseLimit) {
            summary.append(" (+").append(clauses.length - clauseLimit).append(" more)");
        }

        return truncate(summary.toString(), 72);
    }

    private static String cleanClause(String clause) {
        String trimmedClause = clause.trim();
        if (trimmedClause.regionMatches(true, 0, "yes ", 0, 4)) {
            return trimmedClause.substring(4).trim();
        }
        if (trimmedClause.regionMatches(true, 0, "no ", 0, 3)) {
            return "not " + trimmedClause.substring(3).trim();
        }
        return trimmedClause;
    }

    private static String abbreviateTicker(String ticker) {
        if (ticker.length() <= 18) {
            return ticker;
        }
        return ticker.substring(0, 10) + "..." + ticker.substring(ticker.length() - 5);
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
