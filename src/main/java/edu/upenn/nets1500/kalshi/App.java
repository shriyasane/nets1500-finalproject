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
            List<List<Market>> components = graph.connectedComponents();
            printTraversalDemo(graph, markets.get(0).ticker());
            printConnectedComponents(components);
            printDegreeRanking(graph);
            printPathFindingDemo(graph, pickPathDemoComponent(components));
            printNeighborPreview(graph);
        } catch (IOException e) {
            System.err.println("Failed to fetch Kalshi markets: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Kalshi fetch was interrupted.");
        }
    }

    private static void printTraversalDemo(MarketGraph graph, String ticker) {
        Market startMarket = graph.market(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + ticker));

        System.out.println();
        System.out.println("DEMO 1: Graph traversal demo start at Market:");
        printMarketDetails(startMarket, "");
        System.out.println();

        List<Market> breadthFirst = graph.breadthFirstTraversal(ticker);
        System.out.println("DEMO 1A: BFS order:");
        printTraversalOrder(breadthFirst);

        List<Market> depthFirst = graph.depthFirstTraversal(ticker);
        System.out.println("DEMO 1B: DFS order:");
        printTraversalOrder(depthFirst);
        System.out.println("\n\n");
    }

    private static void printConnectedComponents(List<List<Market>> components) {
        System.out.println();
        System.out.println("DEMO 2: Groups of connected components:");

        for (int i = 0; i < components.size(); i++) {
            List<Market> component = components.get(i);
            System.out.printf("Component %d (%d markets):%n", i + 1, component.size());
            for (Market market : component) {
                printMarketDetails(market, "  ");
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }

    private static void printDegreeRanking(MarketGraph graph) {
        System.out.println("DEMO 3: Degree ranking (in order of most connected to least connected Markets):");

        List<Market> ranking = graph.degreeRanking();
        for (int i = 0; i < ranking.size(); i++) {
            Market market = ranking.get(i);
            System.out.printf("%d.%n", i + 1);
            printMarketDetails(market, "  ");
            System.out.printf("  degree: %d%n", graph.neighborsOf(market.ticker()).size());
        }
        System.out.println("\n\n");
    }

    private static void printPathFindingDemo(MarketGraph graph, List<Market> component) {
        if (component.size() < 2) {
            System.out.println("Finding shortest weighted path:");
            System.out.println("No connected component has at least two markets, so no path demo is available.");
            System.out.println();
            return;
        }

        String startTicker = component.get(0).ticker();
        String endTicker = component.get(component.size() - 1).ticker();

        System.out.println("DEMO 4: Finding shortest weighted path:");
        System.out.println("Start market:");
        printMarketDetails(graph.market(startTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + startTicker)), "  ");
        System.out.println("End market:");
        printMarketDetails(graph.market(endTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + endTicker)), "  ");
        System.out.println();

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
        System.out.println("\n\n");
    }

    private static List<Market> pickPathDemoComponent(List<List<Market>> components) {
        return components.stream()
                .filter(component -> component.size() >= 2)
                .max((left, right) -> Integer.compare(left.size(), right.size()))
                .orElse(List.of());
    }

    private static void printNeighborPreview(MarketGraph graph) {
        System.out.println("DEMO 5: Top neighbors for each market:");
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
        System.out.println("\n\n");
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
