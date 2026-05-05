package edu.upenn.nets1500.kalshi.similarity;

import edu.upenn.nets1500.kalshi.model.Market;

public interface MarketSimilarityService {
    double score(Market first, Market second);
}
