package Producer.QueryGenerator;

import MarketDataType.MarketDataQueryType;

/**
 * Interface for generating query messages for different market data sources.
 * Implementations of this interface should provide methods for constructing
 * query messages, defining the data source URL, and assigning a unique tag
 * for identification.
 */
public interface QueryGenerator {

    /**
     * Generates a query message based on the specified product type and query type.
     *
     * @param productType the product or trading pair for which data is requested (e.g., "BTC-USD").
     * @param queryType   the type of market data requested (e.g., trade, quote.
     * @return a string representing the formatted query message.
     */
    String generateQueryMessage(String productType, MarketDataQueryType queryType);


    /**
     * Retrieves the WebSocket or HTTP URL for connecting to the data source.
     *
     * @return the URL as a string.
     */
    String getUrl();

    /**
     * Retrieves a unique identifier or tag associated with the query. (e.g. binance.us)
     * This can be useful for distinguishing between different exchanges or data sources.
     *
     * @return the tag as a string.
     */
    String getTag();
}
