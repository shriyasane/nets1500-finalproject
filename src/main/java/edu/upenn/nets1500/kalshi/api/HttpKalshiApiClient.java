package edu.upenn.nets1500.kalshi.api;

import edu.upenn.nets1500.kalshi.config.KalshiApiConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class HttpKalshiApiClient implements KalshiApiClient {
    private final HttpClient httpClient;
    private final KalshiApiConfig config;

    public HttpKalshiApiClient(HttpClient httpClient, KalshiApiConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    @Override
    public String getMarkets(int limit) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildMarketsUri(limit))
                .GET()
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Kalshi API returned status " + response.statusCode());
        }
        return response.body();
    }

    private URI buildMarketsUri(int limit) {
        String sanitizedBaseUrl = config.baseUrl().endsWith("/")
                ? config.baseUrl().substring(0, config.baseUrl().length() - 1)
                : config.baseUrl();
        int sanitizedLimit = limit > 0 ? Math.min(limit, config.pageSize()) : config.pageSize();
        String encodedLimit = URLEncoder.encode(String.valueOf(sanitizedLimit), StandardCharsets.UTF_8);
        return URI.create(sanitizedBaseUrl + "/markets?limit=" + encodedLimit);
    }
}
