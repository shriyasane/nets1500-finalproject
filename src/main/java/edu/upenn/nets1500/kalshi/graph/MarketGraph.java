package edu.upenn.nets1500.kalshi.graph;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;

public class MarketGraph {
    private final Map<String, Market> marketsByTicker;
    private final Map<String, List<MarketEdge>> adjacencyByTicker;
    private final List<MarketEdge> edges;

    public MarketGraph(Collection<Market> markets, Collection<MarketEdge> edges) {
        this.marketsByTicker = new LinkedHashMap<>();
        this.adjacencyByTicker = new LinkedHashMap<>();
        this.edges = new ArrayList<>();

        for (Market market : markets) {
            marketsByTicker.put(market.ticker(), market);
            adjacencyByTicker.put(market.ticker(), new ArrayList<>());
        }

        for (MarketEdge edge : edges) {
            if (!marketsByTicker.containsKey(edge.sourceTicker()) || !marketsByTicker.containsKey(edge.targetTicker())) {
                throw new IllegalArgumentException("All edges must reference markets present in the graph");
            }

            this.edges.add(edge);
            adjacencyByTicker.get(edge.sourceTicker()).add(edge);
            adjacencyByTicker.get(edge.targetTicker()).add(edge);
        }

        this.edges.sort((left, right) -> Double.compare(right.similarityScore(), left.similarityScore()));
        for (List<MarketEdge> adjacency : adjacencyByTicker.values()) {
            adjacency.sort((left, right) -> Double.compare(right.similarityScore(), left.similarityScore()));
        }
    }

    public Collection<Market> markets() {
        return Collections.unmodifiableCollection(marketsByTicker.values());
    }

    public List<MarketEdge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public Optional<Market> market(String ticker) {
        return Optional.ofNullable(marketsByTicker.get(ticker));
    }

    public List<MarketEdge> neighborsOf(String ticker) {
        List<MarketEdge> neighbors = adjacencyByTicker.get(ticker);
        if (neighbors == null) {
            throw new IllegalArgumentException("Unknown market ticker: " + ticker);
        }
        return Collections.unmodifiableList(neighbors);
    }

    public Set<String> neighborTickersOf(String ticker) {
        Set<String> neighborTickers = new LinkedHashSet<>();
        for (MarketEdge edge : neighborsOf(ticker)) {
            neighborTickers.add(otherEndpoint(ticker, edge));
        }
        return Collections.unmodifiableSet(neighborTickers);
    }

    public Optional<Neighbor> nearestNeighborOf(String ticker) {
        return nearestNeighborsOf(ticker, 1).stream().findFirst();
    }

    public List<Neighbor> nearestNeighborsOf(String ticker) {
        int degree = degreeOf(ticker);
        if (degree == 0) {
            return List.of();
        }
        return nearestNeighborsOf(ticker, degree);
    }

    public List<Neighbor> nearestNeighborsOf(String ticker, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        Market market = market(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + ticker));

        return neighborsOf(market.ticker()).stream()
                .limit(Math.min(limit, degreeOf(market.ticker())))
                .map(edge -> toNeighbor(market.ticker(), edge))
                .toList();
    }

    public List<Market> breadthFirstTraversal(String startTicker) {
        Market startMarket = market(startTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + startTicker));

        List<Market> traversalOrder = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visitedTickers = new HashSet<>();

        queue.addLast(startMarket.ticker());
        visitedTickers.add(startMarket.ticker());

        while (!queue.isEmpty()) {
            String currentTicker = queue.removeFirst();
            traversalOrder.add(requiredMarket(currentTicker));

            for (String neighborTicker : neighborTickersInTraversalOrder(currentTicker)) {
                if (visitedTickers.add(neighborTicker)) {
                    queue.addLast(neighborTicker);
                }
            }
        }

        return List.copyOf(traversalOrder);
    }

    public List<Market> depthFirstTraversal(String startTicker) {
        Market startMarket = market(startTicker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + startTicker));

        List<Market> traversalOrder = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> visitedTickers = new HashSet<>();

        stack.addLast(startMarket.ticker());

        while (!stack.isEmpty()) {
            String currentTicker = stack.removeLast();
            if (!visitedTickers.add(currentTicker)) {
                continue;
            }

            traversalOrder.add(requiredMarket(currentTicker));

            List<String> neighborTickers = neighborTickersInTraversalOrder(currentTicker);
            for (int i = neighborTickers.size() - 1; i >= 0; i--) {
                String neighborTicker = neighborTickers.get(i);
                if (!visitedTickers.contains(neighborTicker)) {
                    stack.addLast(neighborTicker);
                }
            }
        }

        return List.copyOf(traversalOrder);
    }

    public List<List<Market>> connectedComponents() {
        List<List<Market>> components = new ArrayList<>();
        Set<String> visitedTickers = new HashSet<>();

        for (Market market : marketsByTicker.values()) {
            if (visitedTickers.contains(market.ticker())) {
                continue;
            }

            List<Market> component = new ArrayList<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.addLast(market.ticker());
            visitedTickers.add(market.ticker());

            while (!queue.isEmpty()) {
                String currentTicker = queue.removeFirst();
                component.add(requiredMarket(currentTicker));

                for (String neighborTicker : neighborTickersInTraversalOrder(currentTicker)) {
                    if (visitedTickers.add(neighborTicker)) {
                        queue.addLast(neighborTicker);
                    }
                }
            }

            components.add(List.copyOf(component));
        }

        return List.copyOf(components);
    }

    public List<DegreeRankingEntry> degreeRanking() {
        return marketsByTicker.values().stream()
                .map(market -> new DegreeRankingEntry(market, degreeOf(market.ticker())))
                .sorted(Comparator
                        .comparingInt(DegreeRankingEntry::degree)
                        .reversed()
                        .thenComparing(entry -> entry.market().ticker()))
                .toList();
    }

    public List<ClosenessCentralityEntry> closenessCentralityRanking() {
        return marketsByTicker.values().stream()
                .map(market -> new ClosenessCentralityEntry(market, closenessCentralityOf(market.ticker())))
                .sorted(Comparator
                        .comparingDouble(ClosenessCentralityEntry::score)
                        .reversed()
                        .thenComparing(entry -> entry.market().ticker()))
                .toList();
    }

    public List<Market> shortestPathBetween(String startTicker, String endTicker) {
        requiredMarket(startTicker);
        requiredMarket(endTicker);

        if (startTicker.equals(endTicker)) {
            return List.of(requiredMarket(startTicker));
        }

        Map<String, String> previousTicker = new HashMap<>();
        Set<String> visitedTickers = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        queue.addLast(startTicker);
        visitedTickers.add(startTicker);

        while (!queue.isEmpty()) {
            String currentTicker = queue.removeFirst();

            for (String neighborTicker : neighborTickersInTraversalOrder(currentTicker)) {
                if (!visitedTickers.add(neighborTicker)) {
                    continue;
                }

                previousTicker.put(neighborTicker, currentTicker);
                if (neighborTicker.equals(endTicker)) {
                    return reconstructPath(startTicker, endTicker, previousTicker);
                }
                queue.addLast(neighborTicker);
            }
        }

        return List.of();
    }

    public int marketCount() {
        return marketsByTicker.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean hasEdge(String firstTicker, String secondTicker) {
        return neighborsOf(firstTicker).stream()
                .anyMatch(edge -> otherEndpoint(firstTicker, edge).equals(secondTicker));
    }

    public int degreeOf(String ticker) {
        return neighborsOf(ticker).size();
    }

    public String otherEndpoint(String ticker, MarketEdge edge) {
        if (edge.sourceTicker().equals(ticker)) {
            return edge.targetTicker();
        }
        if (edge.targetTicker().equals(ticker)) {
            return edge.sourceTicker();
        }
        throw new IllegalArgumentException("Ticker " + ticker + " is not part of the provided edge");
    }

    private List<String> neighborTickersInTraversalOrder(String ticker) {
        List<String> neighborTickers = new ArrayList<>();
        for (MarketEdge edge : neighborsOf(ticker)) {
            neighborTickers.add(otherEndpoint(ticker, edge));
        }
        return neighborTickers;
    }

    private double closenessCentralityOf(String startTicker) {
        Map<String, Integer> distances = shortestPathDistancesFrom(startTicker);
        if (distances.size() <= 1) {
            return 0.0;
        }

        int totalDistance = distances.values().stream()
                .filter(distance -> distance > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (totalDistance == 0) {
            return 0.0;
        }

        int reachableMarkets = distances.size() - 1;
        return (double) reachableMarkets / totalDistance;
    }

    private Map<String, Integer> shortestPathDistancesFrom(String startTicker) {
        requiredMarket(startTicker);

        Map<String, Integer> distances = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        distances.put(startTicker, 0);
        queue.addLast(startTicker);

        while (!queue.isEmpty()) {
            String currentTicker = queue.removeFirst();
            int currentDistance = distances.get(currentTicker);

            for (String neighborTicker : neighborTickersInTraversalOrder(currentTicker)) {
                if (distances.containsKey(neighborTicker)) {
                    continue;
                }
                distances.put(neighborTicker, currentDistance + 1);
                queue.addLast(neighborTicker);
            }
        }

        return distances;
    }

    private List<Market> reconstructPath(String startTicker, String endTicker, Map<String, String> previousTicker) {
        Deque<Market> path = new ArrayDeque<>();
        String currentTicker = endTicker;

        while (currentTicker != null) {
            path.addFirst(requiredMarket(currentTicker));
            if (currentTicker.equals(startTicker)) {
                return List.copyOf(path);
            }
            currentTicker = previousTicker.get(currentTicker);
        }

        return List.of();
    }

    private Market requiredMarket(String ticker) {
        return market(ticker)
                .orElseThrow(() -> new IllegalStateException("Missing market for ticker: " + ticker));
    }

    private Neighbor toNeighbor(String ticker, MarketEdge edge) {
        String neighborTicker = otherEndpoint(ticker, edge);
        Market neighborMarket = requiredMarket(neighborTicker);
        return new Neighbor(neighborMarket, edge.similarityScore());
    }

    public record Neighbor(Market market, double similarityScore) {
        public Neighbor {
            if (market == null) {
                throw new IllegalArgumentException("market must not be null");
            }
            if (similarityScore < 0.0 || similarityScore > 1.0) {
                throw new IllegalArgumentException("similarityScore must be between 0.0 and 1.0");
            }
        }
    }

    public record DegreeRankingEntry(Market market, int degree) {
        public DegreeRankingEntry {
            if (market == null) {
                throw new IllegalArgumentException("market must not be null");
            }
            if (degree < 0) {
                throw new IllegalArgumentException("degree must not be negative");
            }
        }
    }

    public record ClosenessCentralityEntry(Market market, double score) {
        public ClosenessCentralityEntry {
            if (market == null) {
                throw new IllegalArgumentException("market must not be null");
            }
            if (score < 0.0) {
                throw new IllegalArgumentException("score must not be negative");
            }
        }
    }
}
