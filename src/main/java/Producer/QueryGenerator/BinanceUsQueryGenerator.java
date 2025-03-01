package Producer.QueryGenerator;

import MarketDataType.MarketDataQueryType;


/**
 * Implementation of {@link QueryGenerator} for generating query messages specific to Binance.US WebSocket API.
 * This class constructs subscription messages for different types of market data and provides
 * the WebSocket URL for Binance.US.
 */
public class BinanceUsQueryGenerator implements QueryGenerator {
    /**
     * A counter used to generate unique subscription IDs for WebSocket requests.
     * Each request must have a unique ID.
     */
    private static int idCounter = 1;

    /**
     * Generates a query message for subscribing to Binance.US WebSocket streams.
     *
     * @param productType the product type to subscribe to (e.g., "btcusdt").
     * @param queryType   the type of market data subscription (e.g., TRADE, QUOTE).
     * @return a JSON-formatted query message as a string.
     */
    @Override
    public String generateQueryMessage(String productType, MarketDataQueryType queryType) {
        String param = productType + "@" + (switch (queryType) {
            case TRADE -> "trade";
            case QUOTE -> "bookTicker";
        });
        String subscribeMessage = String.format("""
                {
                    "method": "SUBSCRIBE",
                    "params": ["%s"],
                    "id": %d
                }
                """, param, idCounter);
        idCounter += 1;
        return subscribeMessage;
    }

    /**
     * Retrieves the WebSocket URL for Binance.US's market data feed.
     *
     * @return the WebSocket URL as a string.
     */
    @Override
    public String getUrl() {
        return "wss://stream.binance.us:9443/ws";
    }

    /**
     * Retrieves a unique identifier or tag for this query generator, indicating it is for Binance.US.
     *
     * @return the string "binance.us" as an identifier.
     */
    @Override
    public String getTag() {
        return "binance.us";
    }
}
