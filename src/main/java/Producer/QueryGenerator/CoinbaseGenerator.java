package Producer.QueryGenerator;

import MarketDataType.MarketDataQueryType;

/**
 * Implementation of {@link QueryGenerator} for generating query messages specific to Coinbase's WebSocket API.
 * This class constructs subscription messages for different types of market data and provides the
 * WebSocket URL for Coinbase's exchange.
 */
public class CoinbaseGenerator implements QueryGenerator {
    /**
     * Generates a query message for subscribing to Coinbase's WebSocket feed.
     *
     * @param productType the product type to subscribe to (e.g. "BTC-USD")
     * @param queryType   the type of query to subscribe to (e.g. TRADE, QUOTE)
     * @return a JSON-formatted query message
     */
    @Override
    public String generateQueryMessage(String productType, MarketDataQueryType queryType) {
        String channel = switch (queryType) {
            case TRADE -> "matches";
            case QUOTE -> "ticker";
        };
        return String.format("""
                {
                    "type": "subscribe",
                    "product_ids": ["%s"],
                    "channels": ["%s"]
                }
                """, productType, channel);
    }

    /**
     * Retrieves the WebSocket URL for Coinbase's market data feed.
     *
     * @return the WebSocket URL as a string.
     */
    @Override
    public String getUrl() {
        return "wss://ws-feed.exchange.coinbase.com";
    }


    /**
     * Retrieves a unique identifier or tag for this query generator, indicating it is for Coinbase.
     *
     * @return the string "coinbase" as an identifier.
     */
    @Override
    public String getTag() {
        return "coinbase";
    }

}
