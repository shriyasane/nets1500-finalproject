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
}
