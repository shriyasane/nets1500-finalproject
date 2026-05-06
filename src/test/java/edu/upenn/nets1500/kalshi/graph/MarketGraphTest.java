package edu.upenn.nets1500.kalshi.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketEdge;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.util.List;
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
