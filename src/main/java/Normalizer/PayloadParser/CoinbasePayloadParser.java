package Normalizer.PayloadParser;

import MarketDataType.Ticker;
import MarketDataType.Trade;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.ZonedDateTime;
import java.math.BigDecimal;

public class CoinbasePayloadParser implements Parser {

    private final String platform = "coinbase";

    @Override
    public Ticker parseTicker(JsonNode root) {
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

        var sequence = root.get("sequence").asLong();
        var product = root.get("product_id").asText();
        var bestBid = root.get("best_bid").asText();
        var bestBidSize = root.get("best_bid_size").asText();
        var bestAsk = root.get("best_ask").asText();
        var bestAskSize = root.get("best_ask_size").asText();
        var price = root.get("price").asText();
        var open24h = root.get("open_24h").asText();
        var volume24h = root.get("volume_24h").asText();
        var low24h = root.get("low_24h").asText();
        var high24h = root.get("high_24h").asText();
        var volume30d = root.get("volume_30d").asText();
        var side = root.get("side").asText();
        var time = root.get("time").asText();
        var tradeId = root.get("trade_id").asLong();
        var lastSize = root.get("last_size").asText();
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
        return new Ticker(
                platform,
                sequence,
                product.replace("-", ""),
                new BigDecimal(bestBid),
                new BigDecimal(bestBidSize),
                new BigDecimal(bestAsk),
                new BigDecimal(bestAskSize),
                new BigDecimal(price),
                new BigDecimal(open24h),
                new BigDecimal(volume24h),
                new BigDecimal(low24h),
                new BigDecimal(high24h),
                new BigDecimal(volume30d),
                side,
                ZonedDateTime.parse(time).toInstant(),
                String.valueOf(tradeId),
                new BigDecimal(lastSize)
        );
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

        var tradeId = root.get("trade_id").asLong();
        var sequence = root.get("sequence").asLong();
        var makerOrderId = root.get("maker_order_id").asText();
        var takerOrderId = root.get("taker_order_id").asText();
        var time = root.get("time").asText();
        var product = root.get("product_id").asText();
        var size = root.get("size").asText();
        var price = root.get("price").asText();
        var side = root.get("side").asText();
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
                product.replace("-", ""),
                tradeId,
                new BigDecimal(price),
                new BigDecimal(size),
                makerOrderId,
                takerOrderId,
                side,
                ZonedDateTime.parse(time).toInstant(),
                null
        );

    }
}
