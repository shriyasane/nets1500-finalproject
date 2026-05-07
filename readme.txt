Project Description
This project models the relationship between markets on the prediction market app Kalshi. 
It retrieves market information through the public Kalshi API. 
Through the KalshiMarketFetcher.java file, we select which markets to include in our model. 
We decided this by grouping the markets by categories and then limiting the category count to 5 and the total market count to 15. 
This ensures that the chosen markets will have interesting enough relationships to model via our graph algorithms. 
Market.java then normalizes this data, and TokenJaccardMarketSimilarityService.java tokenizes the markets and produces 
a similarity score by dividing the intersection of tokens between two markets by the union of their tokens. 
This is all used in GraphBuilder.java, where we score every pair of markets, keep only the ones above a certain threshold, 
and then only create edges for the top three (or fewer if fewer exist) scoring markets for each market. MarketGraph.java is 
where we store the final graph. 

After this API to graph pipeline is completed, we call our graph algorithms on the stored graph in App.java. We defined 
all of our graph algorithms and their helper methods in MarketGraph.java. In this class, we have created the following:
- neighborsOf(String ticker)
    - Given a market (via ticker symbol), returns the most similarly connected markets.
- breadthFirstTraversal(String startTicker) and depthFirstTraversal(String startTicker)
    - Allows the user to explore a specific market and see what other markets are in the same cluster.
- connectedComponents()
    - Displays which markets form isolated groups.
- degreeRanking()
    - Allows the user to identify which markets are the most “important” in the graph by seeing which is connected to the most other markets.
- shortestPathBetween(String startTicker, String endTicker)
    - Uses Dijkstra’s algorithm to identify the lowest-cost path between two markets.
These algorithms are run, and the output is printed in the main function. 
We will walk through how to read the output and change user input in the following user manual.


Project Categories Chosen
We chose to do an implementation project, implementing several of the categories provided. 
Our project implements graph algorithms by explicitly modeling Kalshi markets as a graph: 
each market is a node, and an edge is added between two markets when their descriptions are semantically similar above a chosen threshold. 
Once this graph is built, we implemented several graph algorithms to explore the relationships between markets. 
It also implements the information networks (WWW) concept because all of the underlying data comes from Kalshi’s public web API, 
so the graph is built from real-time online market data rather than a static dataset. In addition, our project incorporates elements 
of information retrieval by treating each market description returned by the Kalshi API as a 
text document composed of its title, subtitle, description, and category, then computing token-based Jaccard similarity between markets.

Work Breakdown
Shriya: Data/API pipeline, handle Kalshi API integration, pull and store market data, clean/extract the relevant text fields, 
and prepare the dataset used for similarity scoring, implement the similarity metric

Mona: Graph construction/algorithms, build the graph, and implement the main graph queries/algorithms like nearest neighbors, BFS, 
and degree analysis, develop a detailed readme.txt and user manual

Shared task: both members helped identify a reasonable similarity threshold, test whether the graph relationships are reasonable, 
and debug

AI Usage
Since we never worked with the Kalshi API before, we first reviewed the documentation (https://docs.kalshi.com/api-reference/). 
We then used AI to help setup the configuration and API scaffolding. 