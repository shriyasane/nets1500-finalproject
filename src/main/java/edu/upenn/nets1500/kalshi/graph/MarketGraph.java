package edu.upenn.nets1500.kalshi.graph;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        return nearestNeighborsOf(ticker, degreeOf(ticker));
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

    private Neighbor toNeighbor(String ticker, MarketEdge edge) {
        String neighborTicker = otherEndpoint(ticker, edge);
        Market neighborMarket = market(neighborTicker)
                .orElseThrow(() -> new IllegalStateException("Missing market for ticker: " + neighborTicker));
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
}
