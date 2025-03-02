package Normalizer.PayloadParser;

import MarketDataType.Quote;
import MarketDataType.Trade;
import Utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The {@code BinanceUsPayloadParser} class is responsible for parsing JSON market data messages
 * from Binance.US's WebSocket API into structured {@link Quote} and {@link Trade} objects.
 * <p>
 * This parser handles two types of messages:
 * <ul>
 *     <li>**Order Book Update (Quote)**: Provides best bid and ask prices with their respective sizes.</li>
 *     <li>**Trade Event (Trade)**: Represents an executed trade with price, size, buyer/seller details.</li>
 * </ul>
 */
public class BinanceUsPayloadParser implements Parser {
    private final String platform = "binance.us";
    private static final Logger logger = LogManager.getLogger(BinanceUsPayloadParser.class);

    /**
     * Parses an order book update message from Binance.US WebSocket feed into a {@link Quote} object.
     * <p>
     * The input JSON is expected to contain fields such as:
     * <ul>
     *     <li>{@code u} - Order book update ID</li>
     *     <li>{@code s} - Symbol (e.g., "BNBUSDT")</li>
     *     <li>{@code b}, {@code B} - Best bid price and size</li>
     *     <li>{@code a}, {@code A} - Best ask price and size</li>
     * </ul>
     * If any field is missing or invalid, the method will return {@code null}.
     *
     * @param root the root JSON node containing the order book update data.
     * @return a {@link Quote} object representing the parsed order book update, or {@code null} if parsing fails.
     */
    @Override
    public Quote parseQuote(JsonNode root) {
        //{
        //  "u":400900217,     // order book updateId
        //  "s":"BNBUSDT",     // symbol
        //  "b":"25.35190000", // best bid price
        //  "B":"31.21000000", // best bid qty
        //  "a":"25.36520000", // best ask price
        //  "A":"40.66000000"  // best ask qty
        //}
        try {


            var updateId = JsonUtil.getValue(root.get("u"), JsonNode::asLong);
            var product = JsonUtil.getValue(root.get("s"), JsonNode::asText);
            var bestBid = JsonUtil.getValue(root.get("b"), JsonNode::asText);
            var bestBidSize = JsonUtil.getValue(root.get("B"), JsonNode::asText);
            var bestAsk = JsonUtil.getValue(root.get("a"), JsonNode::asText);
            var bestAskSize = JsonUtil.getValue(root.get("A"), JsonNode::asText);
            //ublic record Quote(
            //        String platform,
            //        long sequence,
            //        String product,
            //        BigDecimal bestBid,
            //        BigDecimal bestBidSize,
            //        BigDecimal bestAsk,
            //        BigDecimal bestAskSize,
            //        BigDecimal price,
            //        BigDecimal open24h,
            //        BigDecimal volume24h,
            //        BigDecimal low24h,
            //        BigDecimal high24h,
            //        BigDecimal volume30d,
            //        String side,
            //        Instant time,
            //        String trade_id,
            //        BigDecimal last_size
            //) implements Serializable {
            //
            //}
            return new Quote(
                    platform,
                    updateId,
                    product,
                    new BigDecimal(bestBid),
                    new BigDecimal(bestBidSize),
                    new BigDecimal(bestAsk),
                    new BigDecimal(bestAskSize),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now(),
                    null,
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to parse trade: {}. Error: {}", root.toString(), e.getMessage());
            return null;
        }
    }

    /**
     * Parses a trade event message from Binance.US WebSocket feed into a {@link Trade} object.
     * <p>
     * The input JSON is expected to contain fields such as:
     * <ul>
     *     <li>{@code e} - Event type (expected to be "trade")</li>
     *     <li>{@code E} - Event timestamp</li>
     *     <li>{@code s} - Symbol (e.g., "BNBBTC")</li>
     *     <li>{@code t} - Trade ID</li>
     *     <li>{@code p}, {@code q} - Price and quantity of trade</li>
     *     <li>{@code b}, {@code a} - Buyer and seller order IDs</li>
     *     <li>{@code T} - Trade timestamp</li>
     *     <li>{@code m} - Whether the buyer is the market maker</li>
     * </ul>
     * If any field is missing or invalid, the method will return {@code null}.
     *
     * @param root the root JSON node containing the trade event data.
     * @return a {@link Trade} object representing the parsed trade, or {@code null} if parsing fails.
     */
    @Override
    public Trade parseTrade(JsonNode root) {
        //{
        //  "e": "trade",     // Event type
        //  "E": 1672515782136,   // Event time
        //  "s": "BNBBTC",    // Symbol
        //  "t": 12345,       // Trade ID
        //  "p": "0.001",     // Price
        //  "q": "100",       // Quantity
        //  "b": 88,          // Buyer order ID
        //  "a": 50,          // Seller order ID
        //  "T": 1672515782136,   // Trade time
        //  "m": true,        // Is the buyer the market maker?
        //  "M": true         // Ignore
        //}
        try {
            var eventTime = JsonUtil.getValue(root.get("E"), JsonNode::asLong);
            var product = JsonUtil.getValue(root.get("s"), JsonNode::asText);
            var tradeId = JsonUtil.getValue(root.get("t"), JsonNode::asLong);
            var price = JsonUtil.getValue(root.get("p"), JsonNode::asText);
            var size = JsonUtil.getValue(root.get("q"), JsonNode::asText);
            var buyerId = JsonUtil.getValue(root.get("b"), JsonNode::asText);
            var sellerId = JsonUtil.getValue(root.get("a"), JsonNode::asText);
            var tradeTime = JsonUtil.getValue(root.get("T"), JsonNode::asLong);
            var buyerIsMarketMaker = JsonUtil.getValue(root.get("m"), JsonNode::asBoolean);

            //public record Trade(
            //        String platform,
            //        Instant eventTime,
            //        String product,
            //        Long tradeId,
            //        BigDecimal price,
            //        BigDecimal size,
            //        String buyerId,
            //        String sellerId,
            //        String side,
            //        Instant tradeTime,
            //        Boolean buyerIsMarketMaker
            //) implements Serializable {
            //}

//        System.out.println(eventTime != null);

            return new Trade(
                    platform,
                    (eventTime != null) ? Instant.ofEpochMilli(eventTime) : null,
                    product,
                    tradeId,
                    (price != null) ? new BigDecimal(price) : null,
                    (size != null) ? new BigDecimal(size) : null,
                    buyerId,
                    sellerId,
                    null,
                    (tradeTime != null) ? Instant.ofEpochMilli(tradeTime) : null,
                    buyerIsMarketMaker
            );
        } catch (Exception e) {
            logger.error("Failed to parse trade: {}. Error: {}", root.toString(), e.getMessage());
            return null;
        }
    }
}
