package edu.upenn.nets1500.kalshi.graph;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
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

        // creates a graph object in adjacency list form by adding edge information to each market's assoc list
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

        // edges for each market are stored in decreasing similarity score order to make graph algos easier to implement
        this.edges.sort((left, right) -> Double.compare(right.similarityScore(), left.similarityScore()));
        for (List<MarketEdge> adjacency : adjacencyByTicker.values()) {
            adjacency.sort((left, right) -> Double.compare(right.similarityScore(), left.similarityScore()));
        }
    }

    // getters
    public Collection<Market> markets() {
        return Collections.unmodifiableCollection(marketsByTicker.values());
    }

    public List<MarketEdge> edges() {
        return Collections.unmodifiableList(edges);
    }

    // returns market object given "key"/ticker
    public Optional<Market> market(String ticker) {
        return Optional.ofNullable(marketsByTicker.get(ticker));
    }

    // given the ticker of a market, returns a list of all edges to neighboring markets (i.e. those that have an 
    // edge to current market)
    public List<MarketEdge> neighborsOf(String ticker) {
        List<MarketEdge> neighbors = adjacencyByTicker.get(ticker);
        if (neighbors == null) {
            throw new IllegalArgumentException("Unknown market ticker: " + ticker);
        }
        return Collections.unmodifiableList(neighbors);
    }

    public List<Neighbor> nearestNeighborsOf(String ticker, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        Market market = market(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown market ticker: " + ticker));

        return neighborsOf(market.ticker()).stream()
                .limit(Math.min(limit, neighborsOf(market.ticker()).size()))
                .map(edge -> toNeighbor(market.ticker(), edge))
                .toList();
    }

    // Breadth-First Search from a given market. Adds neighboring markets to queue, explores neighbors in FIFO order,
    // and pops off until queue empty
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

    // Depth-First Search from a given market. Adds neighboring markets to stack, explores neighbors in LIFO order,
    // and pops off until queue empty
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

    // Performs a pseudo algorithm that runs BFS multiple times until all markets have been reached (each BFS run
    // results in 1 connected component). Returns a list of connected components of market nodes.
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

    // Returns Market sorted by descending in-degree. First Market is most connected (highest degree).
    public List<Market> degreeRanking() {
        return marketsByTicker.values().stream()
                .sorted(Comparator
                        .comparingInt((Market market) -> neighborsOf(market.ticker()).size())
                        .reversed()
                        .thenComparing(Market::ticker))
                .toList();
    }

    // uses Dijkstra's algorithm to find the minimum-cost path between two markets,
    // where each edge cost is defined as 1.0 - similarityScore.
    public List<Market> shortestPathBetween(String startTicker, String endTicker) {
        requiredMarket(startTicker);
        requiredMarket(endTicker);

        if (startTicker.equals(endTicker)) {
            return List.of(requiredMarket(startTicker));
        }

        Map<String, String> previousTicker = new HashMap<>();
        Map<String, Double> bestCostByTicker = new HashMap<>();
        PriorityQueue<Map.Entry<String, Double>> frontier = new PriorityQueue<>(Comparator
                .comparingDouble(Map.Entry<String, Double>::getValue)
                .thenComparing(Map.Entry<String, Double>::getKey));

        bestCostByTicker.put(startTicker, 0.0);
        frontier.add(new AbstractMap.SimpleEntry<>(startTicker, 0.0));

        while (!frontier.isEmpty()) {
            Map.Entry<String, Double> currentState = frontier.remove();
            String currentTicker = currentState.getKey();
            double currentCost = currentState.getValue();

            if (currentCost > bestCostByTicker.getOrDefault(currentTicker, Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (currentTicker.equals(endTicker)) {
                return reconstructPath(startTicker, endTicker, previousTicker);
            }

            for (MarketEdge edge : neighborsOf(currentTicker)) {
                String neighborTicker = otherEndpoint(currentTicker, edge);
                double candidateCost = currentCost + edgeCost(edge);

                // relaxation step of Dijkstra!
                if (candidateCost >= bestCostByTicker.getOrDefault(neighborTicker, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                previousTicker.put(neighborTicker, currentTicker);
                bestCostByTicker.put(neighborTicker, candidateCost);
                frontier.add(new AbstractMap.SimpleEntry<>(neighborTicker, candidateCost));
            }
        }

        return List.of();
    }

    // small helper for App.java printing
    public int marketCount() {
        return marketsByTicker.size();
    }

    // small helper for App.java printing
    public int edgeCount() {
        return edges.size();
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

    // Helper function that returns a list of neighbor tickers in order of decreasing similarity from a particular ticker
    private List<String> neighborTickersInTraversalOrder(String ticker) {
        List<String> neighborTickers = new ArrayList<>();
        for (MarketEdge edge : neighborsOf(ticker)) {
            neighborTickers.add(otherEndpoint(ticker, edge));
        }
        return neighborTickers;
    }

    private double edgeCost(MarketEdge edge) {
        return 1.0 - edge.similarityScore();
    }

    // Dijkstra's helper that follows pointers backwards from endTicker to startTicker to reconstruct the shortest
    // path from start to end after execution
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

    // helper record for nearest-neighbor results: pairs a neighboring market
    // with the similarity score of the edge connecting it to the source market.
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
}
