package edu.upenn.nets1500.kalshi.api;

import java.io.IOException;

public interface KalshiApiClient {
    String getMarkets(int limit) throws IOException, InterruptedException;
    String getMarkets(int limit, String seriesTicker, String status) throws IOException, InterruptedException;
    String getSeries(String category) throws IOException, InterruptedException;
}
