package Normalizer.PayloadParser;

import MarketDataType.Quote;
import MarketDataType.Trade;
import Utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.ZonedDateTime;
import java.math.BigDecimal;

public class CoinbasePayloadParser implements Parser {

    private final String platform = "coinbase";

    @Override
    public Quote parseTicker(JsonNode root) {
        //// Ticker messsage
        //{
        //  "type": "ticker",
        //  "sequence": 37475248783,
        //  "product_id": "ETH-USD",
        //  "price": "1285.22",
        //  "open_24h": "1310.79",
        //  "volume_24h": "245532.79269678",
        //  "low_24h": "1280.52",
        //  "high_24h": "1313.8",
        //  "volume_30d": "9788783.60117027",
        //  "best_bid": "1285.04",
        //  "best_bid_size": "0.46688654",
        //  "best_ask": "1285.27",
        //  "best_ask_size": "1.56637040",
        //  "side": "buy",
        //  "time": "2022-10-19T23:28:22.061769Z",
        //  "trade_id": 370843401,
        //  "last_size": "11.4396987"
        //}
        try {
            var sequence = JsonUtil.getValue(root.get("sequence"), JsonNode::asLong);
            var product = JsonUtil.getValue(root.get("product_id"), JsonNode::asText);
            var bestBid = JsonUtil.getValue(root.get("best_bid"), JsonNode::asText);
            var bestBidSize = JsonUtil.getValue(root.get("best_bid_size"), JsonNode::asText);
            var bestAsk = JsonUtil.getValue(root.get("best_ask"), JsonNode::asText);
            var bestAskSize = JsonUtil.getValue(root.get("best_ask_size"), JsonNode::asText);
            var price = JsonUtil.getValue(root.get("price"), JsonNode::asText);
            var open24h = JsonUtil.getValue(root.get("open_24h"), JsonNode::asText);
            var volume24h = JsonUtil.getValue(root.get("volume_24h"), JsonNode::asText);
            var low24h = JsonUtil.getValue(root.get("low_24h"), JsonNode::asText);
            var high24h = JsonUtil.getValue(root.get("high_24h"), JsonNode::asText);
            var volume30d = JsonUtil.getValue(root.get("volume_30d"), JsonNode::asText);
            var side = JsonUtil.getValue(root.get("side"), JsonNode::asText);
            var time = JsonUtil.getValue(root.get("time"), JsonNode::asText);
            var tradeId = JsonUtil.getValue(root.get("trade_id"), JsonNode::asLong);
            var lastSize = JsonUtil.getValue(root.get("last_size"), JsonNode::asText);
            //public record Ticker(
            //        String platform,
            //        long sequence,
            //        String product,
            //        BigDecimal bestBid,
            //        BigDecimal bestBidSize,
            //        BigDecimal best_ask,
            //        BigDecimal best_ask_size,
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
                    sequence,
                    product.replace("-", ""),
                    (bestBid == null) ? null : new BigDecimal(bestBid),
                    (bestBidSize == null) ? null : new BigDecimal(bestBidSize),
                    (bestAsk == null) ? null : new BigDecimal(bestAsk),
                    (bestAskSize == null) ? null : new BigDecimal(bestAskSize),
                    (price == null) ? null : new BigDecimal(price),
                    (open24h == null) ? null : new BigDecimal(open24h),
                    (volume24h == null) ? null : new BigDecimal(volume24h),
                    (low24h == null) ? null : new BigDecimal(low24h),
                    (high24h == null) ? null : new BigDecimal(high24h),
                    (volume30d == null) ? null : new BigDecimal(volume30d),
                    side,
                    (time == null) ? null : ZonedDateTime.parse(time).toInstant(),
                    (tradeId == null) ? null : String.valueOf(tradeId),
                    (lastSize == null) ? null : new BigDecimal(lastSize)
            );
        } catch (Exception e) {
            System.err.printf("Failed to parse trade: %s\n. Error: %s\n", root.toString(), e.getMessage());
            return null;
        }
    }

    @Override
    public Trade parseTrade(JsonNode root) {
        //{
        //  "type": "match",
        //  "trade_id": 10,
        //  "sequence": 50,
        //  "maker_order_id": "ac928c66-ca53-498f-9c13-a110027a60e8",
        //  "taker_order_id": "132fb6ae-456b-4654-b4e0-d681ac05cea1",
        //  "time": "2014-11-07T08:19:27.028459Z",
        //  "product_id": "BTC-USD",
        //  "size": "5.23512",
        //  "price": "400.23",
        //  "side": "sell"
        //}
        try {
            var tradeId = JsonUtil.getValue(root.get("trade_id"), JsonNode::asLong);
            var sequence = JsonUtil.getValue(root.get("sequence"), JsonNode::asLong);
            var makerOrderId = JsonUtil.getValue(root.get("maker_order_id"), JsonNode::asText);
            var takerOrderId = JsonUtil.getValue(root.get("taker_order_id"), JsonNode::asText);
            var time = JsonUtil.getValue(root.get("time"), JsonNode::asText);
            var product = JsonUtil.getValue(root.get("product_id"), JsonNode::asText);
            var size = JsonUtil.getValue(root.get("size"), JsonNode::asText);
            var price = JsonUtil.getValue(root.get("price"), JsonNode::asText);
            var side = JsonUtil.getValue(root.get("side"), JsonNode::asText);
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
            return new Trade(
                    platform,
                    null,
                    (product == null) ? null : product.replace("-", ""),
                    tradeId,
                    (price == null) ? null : new BigDecimal(price),
                    (size == null) ? null : new BigDecimal(size),
                    makerOrderId,
                    takerOrderId,
                    side,
                    (time == null) ? null : ZonedDateTime.parse(time).toInstant(),
                    null
            );
        } catch (Exception e) {
            System.err.printf("Failed to parse trade: %s\n. Error: %s\n", root.toString(), e.getMessage());
            return null;
        }
    }
}
