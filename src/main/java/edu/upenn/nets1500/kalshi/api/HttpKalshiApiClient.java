package edu.upenn.nets1500.kalshi.api;

import edu.upenn.nets1500.kalshi.config.KalshiApiConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// At a high level, this file builds the URL for the Kalshi /markets endpoint, sends the HTTP request, and returns
// the raw JSON response body as a String
public class HttpKalshiApiClient implements KalshiApiClient {
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MILLIS = 750L;

    private final HttpClient httpClient;
    private final KalshiApiConfig config;

    public HttpKalshiApiClient(HttpClient httpClient, KalshiApiConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    // Sends a GET request to Kalshi's /markets endpoint and returns the raw JSON response body
    @Override
    public String getMarkets(int limit) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildMarketsUri(limit, null, null))
                .GET()
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .build();
        return sendRequest(request);
    }

    @Override
    public String getMarkets(int limit, String seriesTicker, String status) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildMarketsUri(limit, seriesTicker, status))
                .GET()
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .build();
        return sendRequest(request);
    }

    @Override
    public String getSeries(String category) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildSeriesUri(category))
                .GET()
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .build();
        return sendRequest(request);
    }

    // Builds the /markets request URI and clamps the requested limit to the configured page size.
    private URI buildMarketsUri(int limit, String seriesTicker, String status) {
        String sanitizedBaseUrl = config.baseUrl().endsWith("/")
                ? config.baseUrl().substring(0, config.baseUrl().length() - 1)
                : config.baseUrl();
        int sanitizedLimit = limit > 0 ? Math.min(limit, config.pageSize()) : config.pageSize();
        List<String> queryParameters = new ArrayList<>();
        queryParameters.add("limit=" + encode(String.valueOf(sanitizedLimit)));
        if (seriesTicker != null && !seriesTicker.isBlank()) {
            queryParameters.add("series_ticker=" + encode(seriesTicker.trim()));
        }
        if (status != null && !status.isBlank()) {
            queryParameters.add("status=" + encode(status.trim()));
        }
        return URI.create(sanitizedBaseUrl + "/markets?" + String.join("&", queryParameters));
    }

    private URI buildSeriesUri(String category) {
        String sanitizedBaseUrl = config.baseUrl().endsWith("/")
                ? config.baseUrl().substring(0, config.baseUrl().length() - 1)
                : config.baseUrl();
        if (category == null || category.isBlank()) {
            return URI.create(sanitizedBaseUrl + "/series");
        }
        return URI.create(sanitizedBaseUrl + "/series?category=" + encode(category.trim()));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String sendRequest(HttpRequest request) throws IOException, InterruptedException {
        for (int attempt = 0; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }

            if (response.statusCode() == 429 && attempt < MAX_RATE_LIMIT_RETRIES) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }

            throw new IOException("Kalshi API returned status " + response.statusCode());
        }

        throw new IOException("Kalshi API returned status 429");
    }

    private long retryDelayMillis(HttpResponse<String> response, int attempt) {
        return response.headers()
                .firstValue("Retry-After")
                .map(this::parseRetryAfterMillis)
                .orElse(DEFAULT_RETRY_DELAY_MILLIS * (attempt + 1));
    }

    private long parseRetryAfterMillis(String retryAfterHeader) {
        try {
            return Math.max(DEFAULT_RETRY_DELAY_MILLIS, Long.parseLong(retryAfterHeader.trim()) * 1000L);
        } catch (NumberFormatException ignored) {
            return DEFAULT_RETRY_DELAY_MILLIS;
        }
    }
}
