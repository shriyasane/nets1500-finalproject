package edu.upenn.nets1500.kalshi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketTest {
    @Test
    void normalizesOptionalFieldsAndDefaultsStatus() {
        Market market = new Market(
                "  FED-2026  ",
                "  Will the Fed cut rates?  ",
                "  Rates  ",
                "  Policy decision market  ",
                "  economics  ",
                "  FED  ",
                "  FED-DECISION  ",
                null,
                null,
                null,
                null,
                0.42,
                null,
                0.40);

        assertEquals("FED-2026", market.ticker());
        assertEquals("Will the Fed cut rates?", market.title());
        assertEquals("Rates", market.subtitle());
        assertEquals("Policy decision market", market.description());
        assertEquals("economics", market.category());
        assertEquals("FED", market.seriesTicker());
        assertEquals("FED-DECISION", market.eventTicker());
        assertEquals(MarketStatus.UNKNOWN, market.status());
        assertTrue(market.hasPriceData());
        assertFalse(market.hasSchedule());
        assertEquals("Will the Fed cut rates? Rates Policy decision market economics", market.similarityText());
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> new Market(
                " ",
                "Title",
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
                null));
    }

    @Test
    void rejectsInvalidChronology() {
        Instant openTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant closeTime = Instant.parse("2026-01-01T09:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new Market(
                "FED-2026",
                "Will the Fed cut rates?",
                null,
                null,
                null,
                null,
                null,
                MarketStatus.OPEN,
                openTime,
                closeTime,
                null,
                null,
                null,
                null));
    }

    @Test
    void rejectsProbabilitiesOutsideUnitInterval() {
        assertThrows(IllegalArgumentException.class, () -> new Market(
                "FED-2026",
                "Will the Fed cut rates?",
                null,
                null,
                null,
                null,
                null,
                MarketStatus.OPEN,
                null,
                null,
                null,
                1.2,
                null,
                null));
    }
}
