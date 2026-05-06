package edu.upenn.nets1500.kalshi.api;

import edu.upenn.nets1500.kalshi.model.Market;
import edu.upenn.nets1500.kalshi.model.MarketStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KalshiMarketFetcher {
    private static final int DEFAULT_CATEGORY_COUNT = 5;
    private static final List<String> PREFERRED_CATEGORY_ORDER = List.of(
            "Sports",
            "Politics",
            "Economics",
            "Entertainment",
            "Technology",
            "Climate",
            "World");
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

    public List<Market> fetchDiversifiedMarkets(int limit) throws IOException, InterruptedException {
        int sanitizedLimit = Math.max(1, limit);
        List<SeriesSummary> allSeries;
        try {
            allSeries = parseSeries(apiClient.getSeries(null));
        } catch (IOException e) {
            if (isRateLimitError(e)) {
                return fetchMarkets(sanitizedLimit);
            }
            throw e;
        }
        Map<String, List<SeriesSummary>> seriesByCategory = groupSeriesByCategory(allSeries);
        List<String> categories = orderedCategories(seriesByCategory.keySet());
        if (categories.isEmpty()) {
            return fetchMarkets(sanitizedLimit);
        }

        int categoryCount = Math.min(DEFAULT_CATEGORY_COUNT, categories.size());
        int perCategoryLimit = (int) Math.ceil((double) sanitizedLimit / categoryCount);
        List<Market> diversifiedMarkets = new ArrayList<>();
        Set<String> seenTickers = new LinkedHashSet<>();

        for (int i = 0; i < categoryCount && diversifiedMarkets.size() < sanitizedLimit; i++) {
            String category = categories.get(i);
            List<SeriesSummary> categorySeries = seriesByCategory.get(category);
            if (categorySeries == null) {
                continue;
            }

            for (SeriesSummary series : categorySeries) {
                List<Market> markets;
                try {
                    markets = parseMarkets(apiClient.getMarkets(perCategoryLimit, series.ticker(), "open"));
                } catch (IOException e) {
                    if (isRateLimitError(e)) {
                        List<Market> fallbackMarkets = fetchMarkets(sanitizedLimit);
                        appendUnseenMarkets(diversifiedMarkets, seenTickers, fallbackMarkets, sanitizedLimit);
                        return diversifiedMarkets;
                    }
                    throw e;
                }
                if (appendUnseenMarkets(diversifiedMarkets, seenTickers, markets, sanitizedLimit) > 0) {
                    break;
                }
            }
        }

        if (diversifiedMarkets.size() < sanitizedLimit) {
            List<Market> fallbackMarkets = fetchMarkets(sanitizedLimit);
            appendUnseenMarkets(diversifiedMarkets, seenTickers, fallbackMarkets, sanitizedLimit);
        }

        return diversifiedMarkets;
    }

    List<Market> parseMarkets(String responseBody) {
        String marketsArray = extractTopLevelArray(responseBody, "markets");
        List<String> marketObjects = splitTopLevelObjects(marketsArray);
        List<Market> markets = new ArrayList<>();

        for (String marketObject : marketObjects) {
            markets.add(parseMarket(marketObject));
        }

        return markets;
    }

    List<SeriesSummary> parseSeries(String responseBody) {
        String seriesArray = extractTopLevelArray(responseBody, "series");
        List<String> seriesObjects = splitTopLevelObjects(seriesArray);
        List<SeriesSummary> series = new ArrayList<>();

        for (String seriesObject : seriesObjects) {
            String ticker = extractString(seriesObject, "ticker");
            String category = extractString(seriesObject, "category");
            if (ticker != null && category != null) {
                series.add(new SeriesSummary(ticker, category));
            }
        }

        return series;
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

    private Map<String, List<SeriesSummary>> groupSeriesByCategory(List<SeriesSummary> series) {
        Map<String, List<SeriesSummary>> grouped = new LinkedHashMap<>();
        for (SeriesSummary summary : series) {
            grouped.computeIfAbsent(summary.category(), ignored -> new ArrayList<>()).add(summary);
        }
        return grouped;
    }

    private List<String> orderedCategories(Set<String> categories) {
        List<String> ordered = new ArrayList<>();
        for (String preferredCategory : PREFERRED_CATEGORY_ORDER) {
            if (categories.contains(preferredCategory)) {
                ordered.add(preferredCategory);
            }
        }

        categories.stream()
                .filter(category -> !ordered.contains(category))
                .sorted(Comparator.naturalOrder())
                .forEach(ordered::add);
        return ordered;
    }

    private int appendUnseenMarkets(
            List<Market> destination,
            Set<String> seenTickers,
            List<Market> candidateMarkets,
            int limit) {
        int added = 0;
        for (Market market : candidateMarkets) {
            if (destination.size() >= limit) {
                break;
            }
            if (seenTickers.add(market.ticker())) {
                destination.add(market);
                added++;
            }
        }
        return added;
    }

    private String extractTopLevelArray(String responseBody, String fieldName) {
        String json = responseBody == null ? "" : responseBody.trim();
        int keyIndex = json.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Response did not contain a " + fieldName + " array");
        }

        int arrayStart = json.indexOf('[', keyIndex);
        if (arrayStart < 0) {
            throw new IllegalArgumentException("Response did not contain a valid " + fieldName + " array");
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

        throw new IllegalArgumentException("Response did not contain a closed " + fieldName + " array");
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

    private boolean isRateLimitError(IOException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("429");
    }

    record SeriesSummary(String ticker, String category) {
    }
}
