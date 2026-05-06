package edu.upenn.nets1500.kalshi.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import edu.upenn.nets1500.kalshi.similarity.TokenJaccardMarketSimilarityService;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {
    @Test
    void buildsWeightedGraphFromSimilarityScores() {
        GraphBuilder builder = new GraphBuilder(new TokenJaccardMarketSimilarityService(), 0.2, 3);

        Market fedMay = market(
                "FED-MAY",
                "Will the Fed cut rates in May?",
                "Federal Reserve policy decision");
        Market fedJune = market(
                "FED-JUN",
                "Will the Fed cut rates in June?",
                "Federal Reserve interest rate decision");
        Market cpi = market(
                "CPI-JUN",
                "Will CPI exceed 4 percent in June?",
                "Consumer inflation report");

        MarketGraph graph = builder.buildGraph(List.of(fedMay, fedJune, cpi));

        assertEquals(3, graph.marketCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.hasEdge("FED-MAY", "FED-JUN"));
        assertFalse(graph.hasEdge("FED-MAY", "CPI-JUN"));
        assertEquals(1, graph.degreeOf("FED-MAY"));
        assertEquals("FED-JUN", graph.otherEndpoint("FED-MAY", graph.neighborsOf("FED-MAY").get(0)));
    }

    @Test
    void respectsSimilarityThreshold() {
        GraphBuilder builder = new GraphBuilder(new TokenJaccardMarketSimilarityService(), 0.6, 3);

        Market fedMay = market(
                "FED-MAY",
                "Will the Fed cut rates in May?",
                "Federal Reserve policy decision");
        Market fedJune = market(
                "FED-JUN",
                "Will the Fed cut rates in June?",
                "Federal Reserve interest rate decision");

        MarketGraph graph = builder.buildGraph(List.of(fedMay, fedJune));

        assertEquals(0, graph.edgeCount());
    }

    @Test
    void capsNeighborCountPerMarket() {
        GraphBuilder builder = new GraphBuilder(new TokenJaccardMarketSimilarityService(), 0.2, 1);

        Market fedMay = market(
                "FED-MAY",
                "Will the Fed cut rates in May?",
                "Federal Reserve policy decision");
        Market fedJune = market(
                "FED-JUN",
                "Will the Fed cut rates in June?",
                "Federal Reserve policy decision");
        Market fedJuly = market(
                "FED-JUL",
                "Will the Fed cut rates in July?",
                "Federal Reserve policy decision");

        MarketGraph graph = builder.buildGraph(List.of(fedMay, fedJune, fedJuly));

        assertEquals(1, graph.edgeCount());
        assertTrue(graph.degreeOf("FED-MAY") <= 1);
        assertTrue(graph.degreeOf("FED-JUN") <= 1);
        assertTrue(graph.degreeOf("FED-JUL") <= 1);
    }

    private static Market market(String ticker, String title, String description) {
        return new Market(
                ticker,
                title,
                null,
                description,
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
