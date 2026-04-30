package edu.upenn.nets1500.kalshi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class KalshiApiConfigTest {
    @Test
    void appliesDefaultsFromConvenienceConstructor() {
        KalshiApiConfig config = new KalshiApiConfig(" https://api.elections.kalshi.com ");

        assertEquals("https://api.elections.kalshi.com", config.baseUrl());
        assertEquals(Duration.ofSeconds(10), config.requestTimeout());
        assertEquals(100, config.pageSize());
    }

    @Test
    void rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new KalshiApiConfig("https://api.elections.kalshi.com", Duration.ofSeconds(5), 0));
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                new KalshiApiConfig("https://api.elections.kalshi.com", Duration.ZERO, 100));
    }
}
