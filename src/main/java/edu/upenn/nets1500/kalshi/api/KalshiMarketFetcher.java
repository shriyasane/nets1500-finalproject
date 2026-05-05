package edu.upenn.nets1500.kalshi.api;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KalshiMarketFetcher {
    private static final Pattern STRING_FIELD_TEMPLATE =
            Pattern.compile("\"%s\"\\s*:\\s*(null|\"((?:\\\\.|[^\\\\\"])*)\")");

    private final KalshiApiClient apiClient;

    public KalshiMarketFetcher(KalshiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<Market> fetchMarkets() throws IOException, InterruptedException {
        return fetchMarkets(100);
    }

    public List<Market> fetchMarkets(int limit) throws IOException, InterruptedException {
        String responseBody = apiClient.getMarkets(limit);
        return parseMarkets(responseBody);
    }

    List<Market> parseMarkets(String responseBody) {
        String marketsArray = extractMarketsArray(responseBody);
        List<String> marketObjects = splitTopLevelObjects(marketsArray);
        List<Market> markets = new ArrayList<>();

        for (String marketObject : marketObjects) {
            markets.add(parseMarket(marketObject));
        }

        return markets;
    }

    private Market parseMarket(String marketJson) {
        return new Market(
                extractString(marketJson, "ticker"),
                extractString(marketJson, "title"),
                extractString(marketJson, "subtitle"),
                extractString(marketJson, "rules_primary"),
                null,
                null,
                extractString(marketJson, "event_ticker"),
                MarketStatus.fromApiValue(extractString(marketJson, "status")),
                parseInstant(extractString(marketJson, "open_time")),
                parseInstant(extractString(marketJson, "close_time")),
                parseInstant(extractString(marketJson, "expiration_time")),
                parseDouble(extractString(marketJson, "yes_ask_dollars")),
                parseDouble(extractString(marketJson, "no_ask_dollars")),
                parseDouble(extractString(marketJson, "last_price_dollars")));
    }

    private String extractMarketsArray(String responseBody) {
        String json = responseBody == null ? "" : responseBody.trim();
        int keyIndex = json.indexOf("\"markets\"");
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Response did not contain a markets array");
        }

        int arrayStart = json.indexOf('[', keyIndex);
        if (arrayStart < 0) {
            throw new IllegalArgumentException("Response did not contain a valid markets array");
        }

        int depth = 0;
        boolean inString = false;
        for (int i = arrayStart; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '"' && !isEscaped(json, i)) {
                inString = !inString;
            }

            if (inString) {
                continue;
            }

            if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(arrayStart + 1, i);
                }
            }
        }

        throw new IllegalArgumentException("Response did not contain a closed markets array");
    }

    private List<String> splitTopLevelObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        int objectStart = -1;
        int depth = 0;
        boolean inString = false;

        for (int i = 0; i < arrayJson.length(); i++) {
            char current = arrayJson.charAt(i);
            if (current == '"' && !isEscaped(arrayJson, i)) {
                inString = !inString;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(arrayJson.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            }
        }

        return objects;
    }

    private String extractString(String json, String fieldName) {
        Pattern fieldPattern = Pattern.compile(String.format(STRING_FIELD_TEMPLATE.pattern(), Pattern.quote(fieldName)));
        Matcher matcher = fieldPattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        if ("null".equals(matcher.group(1))) {
            return null;
        }
        return matcher.group(2)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private Double parseDouble(String value) {
        return value == null ? null : Double.parseDouble(value);
    }

    private boolean isEscaped(String text, int index) {
        int backslashCount = 0;
        for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            backslashCount++;
        }
        return backslashCount % 2 == 1;
    }
}
