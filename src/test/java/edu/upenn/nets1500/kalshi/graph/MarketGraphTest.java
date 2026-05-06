package edu.upenn.nets1500.kalshi.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketGraphTest {
    @Test
    void storesUndirectedAdjacencyForEachEdge() {
        Market first = market("FED-MAY", "Will the Fed cut rates in May?");
        Market second = market("FED-JUN", "Will the Fed cut rates in June?");
        Market third = market("CPI-JUN", "Will CPI exceed 4 percent in June?");

        MarketEdge fedEdge = new MarketEdge("FED-MAY", "FED-JUN", 0.8);
        MarketEdge cpiEdge = new MarketEdge("FED-JUN", "CPI-JUN", 0.3);

        MarketGraph graph = new MarketGraph(List.of(first, second, third), List.of(fedEdge, cpiEdge));

        assertEquals(3, graph.marketCount());
        assertEquals(2, graph.edgeCount());
        assertEquals(1, graph.degreeOf("FED-MAY"));
        assertEquals(2, graph.degreeOf("FED-JUN"));
        assertEquals(1, graph.degreeOf("CPI-JUN"));
        assertEquals("FED-JUN", graph.otherEndpoint("FED-MAY", graph.neighborsOf("FED-MAY").get(0)));
        assertTrue(graph.neighborTickersOf("FED-JUN").contains("FED-MAY"));
        assertTrue(graph.neighborTickersOf("FED-JUN").contains("CPI-JUN"));
    }

    @Test
    void returnsNearestNeighborsOrderedBySimilarity() {
        Market fedMay = market("FED-MAY", "Will the Fed cut rates in May?");
        Market fedJune = market("FED-JUN", "Will the Fed cut rates in June?");
        Market cpiJune = market("CPI-JUN", "Will CPI exceed 4 percent in June?");

        MarketGraph graph = new MarketGraph(
                List.of(fedMay, fedJune, cpiJune),
                List.of(
                        new MarketEdge("FED-MAY", "CPI-JUN", 0.3),
                        new MarketEdge("FED-MAY", "FED-JUN", 0.8)));

        List<MarketGraph.Neighbor> neighbors = graph.nearestNeighborsOf("FED-MAY");

        assertEquals(2, neighbors.size());
        assertEquals("FED-JUN", neighbors.get(0).market().ticker());
        assertEquals(0.8, neighbors.get(0).similarityScore());
        assertEquals("CPI-JUN", neighbors.get(1).market().ticker());
        assertEquals(0.3, neighbors.get(1).similarityScore());
    }

    @Test
    void respectsRequestedNearestNeighborLimit() {
        Market fedMay = market("FED-MAY", "Will the Fed cut rates in May?");
        Market fedJune = market("FED-JUN", "Will the Fed cut rates in June?");
        Market fedJuly = market("FED-JUL", "Will the Fed cut rates in July?");

        MarketGraph graph = new MarketGraph(
                List.of(fedMay, fedJune, fedJuly),
                List.of(
                        new MarketEdge("FED-MAY", "FED-JUL", 0.7),
                        new MarketEdge("FED-MAY", "FED-JUN", 0.9)));

        List<MarketGraph.Neighbor> neighbors = graph.nearestNeighborsOf("FED-MAY", 1);

        assertEquals(1, neighbors.size());
        assertEquals("FED-JUN", neighbors.get(0).market().ticker());
    }

    @Test
    void returnsEmptyNearestNeighborForIsolatedMarket() {
        Market isolated = market("ISOLATED", "Will this market remain isolated?");
        Market other = market("OTHER", "Another unrelated market");
        MarketGraph graph = new MarketGraph(List.of(isolated, other), List.of());

        assertTrue(graph.nearestNeighborsOf("ISOLATED").isEmpty());

        Optional<MarketGraph.Neighbor> nearest = graph.nearestNeighborOf("ISOLATED");
        assertFalse(nearest.isPresent());
    }

    @Test
    void rejectsNonPositiveNearestNeighborLimit() {
        Market fedMay = market("FED-MAY", "Will the Fed cut rates in May?");
        MarketGraph graph = new MarketGraph(List.of(fedMay), List.of());

        assertThrows(IllegalArgumentException.class, () -> graph.nearestNeighborsOf("FED-MAY", 0));
    }

    @Test
    void breadthFirstTraversalVisitsMarketsLevelByLevel() {
        Market start = market("START", "Starting market");
        Market branchA = market("A", "Branch A");
        Market branchB = market("B", "Branch B");
        Market branchC = market("C", "Branch C");
        Market tail = market("TAIL", "Tail market");

        MarketGraph graph = new MarketGraph(
                List.of(start, branchA, branchB, branchC, tail),
                List.of(
                        new MarketEdge("START", "A", 0.9),
                        new MarketEdge("START", "B", 0.8),
                        new MarketEdge("A", "C", 0.7),
                        new MarketEdge("B", "TAIL", 0.6)));

        List<Market> traversal = graph.breadthFirstTraversal("START");

        assertEquals(List.of("START", "A", "B", "C", "TAIL"), tickers(traversal));
    }

    @Test
    void depthFirstTraversalFollowsOneBranchBeforeBacktracking() {
        Market start = market("START", "Starting market");
        Market branchA = market("A", "Branch A");
        Market branchB = market("B", "Branch B");
        Market branchC = market("C", "Branch C");
        Market tail = market("TAIL", "Tail market");

        MarketGraph graph = new MarketGraph(
                List.of(start, branchA, branchB, branchC, tail),
                List.of(
                        new MarketEdge("START", "A", 0.9),
                        new MarketEdge("START", "B", 0.8),
                        new MarketEdge("A", "C", 0.7),
                        new MarketEdge("B", "TAIL", 0.6)));

        List<Market> traversal = graph.depthFirstTraversal("START");

        assertEquals(List.of("START", "A", "C", "B", "TAIL"), tickers(traversal));
    }

    @Test
    void traversalRejectsUnknownStartTicker() {
        Market fedMay = market("FED-MAY", "Will the Fed cut rates in May?");
        MarketGraph graph = new MarketGraph(List.of(fedMay), List.of());

        assertThrows(IllegalArgumentException.class, () -> graph.breadthFirstTraversal("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> graph.depthFirstTraversal("UNKNOWN"));
    }

    @Test
    void connectedComponentsGroupsMarketsIntoIsolatedClusters() {
        Market a = market("A", "Market A");
        Market b = market("B", "Market B");
        Market c = market("C", "Market C");
        Market d = market("D", "Market D");
        Market e = market("E", "Market E");
        Market isolated = market("ISO", "Isolated market");

        MarketGraph graph = new MarketGraph(
                List.of(a, b, c, d, e, isolated),
                List.of(
                        new MarketEdge("A", "B", 0.9),
                        new MarketEdge("B", "C", 0.8),
                        new MarketEdge("D", "E", 0.7)));

        List<List<Market>> components = graph.connectedComponents();

        assertEquals(3, components.size());
        assertEquals(List.of("A", "B", "C"), tickers(components.get(0)));
        assertEquals(List.of("D", "E"), tickers(components.get(1)));
        assertEquals(List.of("ISO"), tickers(components.get(2)));
    }

    @Test
    void degreeRankingSortsMarketsByDescendingNeighborCount() {
        Market a = market("A", "Market A");
        Market b = market("B", "Market B");
        Market c = market("C", "Market C");
        Market d = market("D", "Market D");

        MarketGraph graph = new MarketGraph(
                List.of(a, b, c, d),
                List.of(
                        new MarketEdge("A", "B", 0.9),
                        new MarketEdge("A", "C", 0.8),
                        new MarketEdge("B", "C", 0.7)));

        List<MarketGraph.DegreeRankingEntry> ranking = graph.degreeRanking();

        assertEquals(List.of("A", "B", "C", "D"),
                ranking.stream().map(entry -> entry.market().ticker()).toList());
        assertEquals(List.of(2, 2, 2, 0),
                ranking.stream().map(MarketGraph.DegreeRankingEntry::degree).toList());
    }

    @Test
    void closenessCentralityRankingPrioritizesMarketsWithShorterAverageDistances() {
        Market a = market("A", "Market A");
        Market b = market("B", "Market B");
        Market c = market("C", "Market C");
        Market d = market("D", "Market D");

        MarketGraph graph = new MarketGraph(
                List.of(a, b, c, d),
                List.of(
                        new MarketEdge("A", "B", 0.9),
                        new MarketEdge("B", "C", 0.8),
                        new MarketEdge("C", "D", 0.7)));

        List<MarketGraph.ClosenessCentralityEntry> ranking = graph.closenessCentralityRanking();

        assertEquals(List.of("B", "C", "A", "D"),
                ranking.stream().map(entry -> entry.market().ticker()).toList());
        assertEquals(List.of(0.75, 0.75, 0.5, 0.5),
                ranking.stream().map(MarketGraph.ClosenessCentralityEntry::score).toList());
    }

    @Test
    void shortestPathBetweenReturnsMarketsAlongMinimumHopPath() {
        Market a = market("A", "Market A");
        Market b = market("B", "Market B");
        Market c = market("C", "Market C");
        Market d = market("D", "Market D");

        MarketGraph graph = new MarketGraph(
                List.of(a, b, c, d),
                List.of(
                        new MarketEdge("A", "B", 0.9),
                        new MarketEdge("B", "C", 0.8),
                        new MarketEdge("C", "D", 0.7)));

        List<Market> path = graph.shortestPathBetween("A", "D");

        assertEquals(List.of("A", "B", "C", "D"), tickers(path));
    }

    @Test
    void shortestPathBetweenReturnsEmptyWhenMarketsAreDisconnected() {
        Market a = market("A", "Market A");
        Market b = market("B", "Market B");
        Market isolated = market("ISO", "Isolated market");

        MarketGraph graph = new MarketGraph(
                List.of(a, b, isolated),
                List.of(new MarketEdge("A", "B", 0.9)));

        assertTrue(graph.shortestPathBetween("A", "ISO").isEmpty());
    }

    private static Market market(String ticker, String title) {
        return new Market(
                ticker,
                title,
                null,
                null,
                null,
                null,
                null,
                MarketStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static List<String> tickers(List<Market> markets) {
        return markets.stream().map(Market::ticker).toList();
    }
}
