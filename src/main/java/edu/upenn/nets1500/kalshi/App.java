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
            List<Market> markets = fetcher.fetchDiversifiedMarkets(DEFAULT_FETCH_LIMIT);
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
            printTraversalDemo(graph, markets.get(0).ticker());
            printConnectedComponents(graph);
            printDegreeRanking(graph);
            printClosenessCentrality(graph);
            printPathFindingDemo(graph, markets.get(0).ticker(), markets.get(markets.size() - 1).ticker());
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
        System.out.println("Nearest neighbors for:");
        printMarketDetails(market, "");
        List<MarketGraph.Neighbor> neighbors = graph.nearestNeighborsOf(ticker, limit);
        if (neighbors.isEmpty()) {
            System.out.println("No similar neighbors above threshold.");
            return;
        }

        for (MarketGraph.Neighbor neighbor : neighbors) {
            System.out.println();
            printMarketDetails(neighbor.market(), "  ");
            System.out.printf("  score: %.2f%n", neighbor.similarityScore());
        }
    }

    private static void printTraversalDemo(MarketGraph graph, String ticker) {
        Market startMarket = graph.market(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + ticker));

        System.out.println();
        System.out.println("Traversal demo from:");
        printMarketDetails(startMarket, "");

        List<Market> breadthFirst = graph.breadthFirstTraversal(ticker);
        System.out.println("BFS order:");
        printTraversalOrder(breadthFirst);

        List<Market> depthFirst = graph.depthFirstTraversal(ticker);
        System.out.println("DFS order:");
        printTraversalOrder(depthFirst);
    }

    private static void printConnectedComponents(MarketGraph graph) {
        System.out.println();
        System.out.println("Connected components:");

        List<List<Market>> components = graph.connectedComponents();
        for (int i = 0; i < components.size(); i++) {
            List<Market> component = components.get(i);
            System.out.printf("Component %d (%d markets):%n", i + 1, component.size());
            for (Market market : component) {
                printMarketDetails(market, "  ");
            }
            System.out.println();
        }
    }

    private static void printDegreeRanking(MarketGraph graph) {
        System.out.println("Degree ranking:");

        List<MarketGraph.DegreeRankingEntry> ranking = graph.degreeRanking();
        for (int i = 0; i < ranking.size(); i++) {
            MarketGraph.DegreeRankingEntry entry = ranking.get(i);
            System.out.printf("%d.%n", i + 1);
            printMarketDetails(entry.market(), "  ");
            System.out.printf("  degree: %d%n", entry.degree());
        }
        System.out.println();
    }

    private static void printClosenessCentrality(MarketGraph graph) {
        System.out.println("Closeness centrality:");

        List<MarketGraph.ClosenessCentralityEntry> ranking = graph.closenessCentralityRanking();
        for (int i = 0; i < ranking.size(); i++) {
            MarketGraph.ClosenessCentralityEntry entry = ranking.get(i);
            System.out.printf("%d.%n", i + 1);
            printMarketDetails(entry.market(), "  ");
            System.out.printf("  closeness score: %.3f%n", entry.score());
        }
        System.out.println();
    }

    private static void printPathFindingDemo(MarketGraph graph, String startTicker, String endTicker) {
        System.out.println("Path finding:");
        System.out.println("Start market:");
        printMarketDetails(graph.market(startTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + startTicker)), "  ");
        System.out.println("End market:");
        printMarketDetails(graph.market(endTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + endTicker)), "  ");

        List<Market> path = graph.shortestPathBetween(startTicker, endTicker);
        if (path.isEmpty()) {
            System.out.println("No path found between these markets.");
            System.out.println();
            return;
        }

        System.out.println("Shortest path:");
        for (int i = 0; i < path.size(); i++) {
            System.out.printf("%d.%n", i + 1);
            printMarketDetails(path.get(i), "  ");
        }
        System.out.println();
    }

    private static void printNeighborPreview(MarketGraph graph) {
        System.out.println("Top neighbors by market:");
        for (Market market : graph.markets()) {
            System.out.println();
            printMarketDetails(market, "");
            List<MarketEdge> neighbors = graph.neighborsOf(market.ticker());
            if (neighbors.isEmpty()) {
                System.out.println("  No similar neighbors above threshold.");
                continue;
            }

            for (int i = 0; i < neighbors.size(); i++) {
                MarketEdge edge = neighbors.get(i);
                Market neighbor = graph.market(graph.otherEndpoint(market.ticker(), edge))
                        .orElseThrow(() -> new IllegalStateException("Missing market for graph edge"));
                System.out.printf("  %d.%n", i + 1);
                printMarketDetails(neighbor, "     ");
                System.out.printf("     score: %.2f%n", edge.similarityScore());
            }
        }
    }

    private static void printMarketDetails(Market market, String indent) {
        System.out.println(indent + buildShortTitle(market));
        System.out.println(indent + "full title: " + market.title());
        System.out.println(indent + "ticker: " + market.ticker());
    }

    private static void printTraversalOrder(List<Market> traversal) {
        for (int i = 0; i < traversal.size(); i++) {
            Market market = traversal.get(i);
            System.out.printf("  %d. %s (%s)%n", i + 1, buildShortTitle(market), market.ticker());
        }
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

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
