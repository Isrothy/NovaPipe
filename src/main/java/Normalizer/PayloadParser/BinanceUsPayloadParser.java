package Normalizer.PayloadParser;

import MarketDataType.Quote;
import MarketDataType.Trade;
import Utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.math.BigDecimal;

public class BinanceUsPayloadParser implements Parser {
    private final String platform = "binance.us";

    @Override
    public Quote parseTicker(JsonNode root) {
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
            System.err.printf("Failed to parse trade: %s\n. Error: %s\n", root.toString(), e.getMessage());
            return null;
        }
    }

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
            System.err.printf("Failed to parse trade: %s\n. Error: %s\n", root.toString(), e.getMessage());
            return null;
        }
    }
}
