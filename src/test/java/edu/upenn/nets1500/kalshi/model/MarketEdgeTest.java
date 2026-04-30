package edu.upenn.nets1500.kalshi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MarketEdgeTest {
    @Test
    void trimsTickersAndKeepsValidSimilarity() {
        MarketEdge edge = new MarketEdge("  A  ", "  B  ", 0.75);

        assertEquals("A", edge.sourceTicker());
        assertEquals("B", edge.targetTicker());
        assertEquals(0.75, edge.similarityScore());
    }

    @Test
    void rejectsOutOfRangeSimilarity() {
        assertThrows(IllegalArgumentException.class, () -> new MarketEdge("A", "B", -0.1));
    }
}
