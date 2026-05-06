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
}
