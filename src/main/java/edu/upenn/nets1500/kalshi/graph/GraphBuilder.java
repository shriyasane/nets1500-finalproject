package edu.upenn.nets1500.kalshi.graph;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import edu.upenn.nets1500.kalshi.similarity.MarketSimilarityService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GraphBuilder {
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.2;
    private static final int DEFAULT_MAX_NEIGHBORS_PER_MARKET = 3;

    private final MarketSimilarityService similarityService;
    private final double similarityThreshold;
    private final int maxNeighborsPerMarket;

    public GraphBuilder(MarketSimilarityService similarityService) {
        this(similarityService, DEFAULT_SIMILARITY_THRESHOLD, DEFAULT_MAX_NEIGHBORS_PER_MARKET);
    }

    public GraphBuilder(
            MarketSimilarityService similarityService,
            double similarityThreshold,
            int maxNeighborsPerMarket) {
        this.similarityService = Objects.requireNonNull(similarityService, "similarityService must not be null");
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("similarityThreshold must be between 0.0 and 1.0");
        }
        if (maxNeighborsPerMarket <= 0) {
            throw new IllegalArgumentException("maxNeighborsPerMarket must be positive");
        }
        this.similarityThreshold = similarityThreshold;
        this.maxNeighborsPerMarket = maxNeighborsPerMarket;
    }

    public MarketGraph buildGraph(Collection<Market> markets) {
        List<Market> marketList = new ArrayList<>(markets);
        Map<String, Integer> degreeByTicker = new LinkedHashMap<>();
        for (Market market : marketList) {
            degreeByTicker.put(market.ticker(), 0);
        }

        List<MarketEdge> candidateEdges = scoreCandidateEdges(marketList);
        List<MarketEdge> selectedEdges = new ArrayList<>();

        for (MarketEdge edge : candidateEdges) {
            if (degreeByTicker.get(edge.sourceTicker()) >= maxNeighborsPerMarket
                    || degreeByTicker.get(edge.targetTicker()) >= maxNeighborsPerMarket) {
                continue;
            }

            selectedEdges.add(edge);
            degreeByTicker.put(edge.sourceTicker(), degreeByTicker.get(edge.sourceTicker()) + 1);
            degreeByTicker.put(edge.targetTicker(), degreeByTicker.get(edge.targetTicker()) + 1);
        }

        return new MarketGraph(marketList, selectedEdges);
    }

    public double similarityThreshold() {
        return similarityThreshold;
    }

    public int maxNeighborsPerMarket() {
        return maxNeighborsPerMarket;
    }

    private List<MarketEdge> scoreCandidateEdges(List<Market> markets) {
        List<MarketEdge> candidates = new ArrayList<>();

        for (int i = 0; i < markets.size(); i++) {
            for (int j = i + 1; j < markets.size(); j++) {
                Market first = markets.get(i);
                Market second = markets.get(j);
                double similarityScore = similarityService.score(first, second);

                if (similarityScore >= similarityThreshold) {
                    candidates.add(new MarketEdge(first.ticker(), second.ticker(), similarityScore));
                }
            }
        }

        candidates.sort(Comparator
                .comparingDouble(MarketEdge::similarityScore)
                .reversed()
                .thenComparing(MarketEdge::sourceTicker)
                .thenComparing(MarketEdge::targetTicker));
        return candidates;
    }
}
