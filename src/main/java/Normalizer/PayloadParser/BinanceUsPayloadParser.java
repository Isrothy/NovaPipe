package Normalizer.PayloadParser;

import MarketDataType.Ticker;
import MarketDataType.Trade;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.math.BigDecimal;

public class BinanceUsPayloadParser implements Parser {

    @Override
    public Ticker parseTicker(JsonNode root) {
        var updateId = root.get("u").asLong();
        var product = root.get("s").asText();
        var bestBid = root.get("b").asText();
        var bestBidSize = root.get("B").asText();
        var bestAsk = root.get("a").asText();
        var bestAskSize = root.get("A").asText();

        return new Ticker(
                "binance.us",
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
        var eventTime = root.get("E").asLong();
        var product = root.get("s").asText();
        var tradeId = root.get("t").asLong();
        var price = root.get("p").asText();
        var size = root.get("q").asText();
        var buyerId = root.get("b").asText();
        var sellerId = root.get("a").asText();
        var tradeTime = root.get("T").asLong();
        var buyerIsMarketMaker = root.get("m").asBoolean();

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
                "binance.us",
                Instant.ofEpochMilli(eventTime),
                product,
                tradeId,
                new BigDecimal(price),
                new BigDecimal(size),
                buyerId,
                sellerId,
                null,
                Instant.ofEpochMilli(tradeTime),
                buyerIsMarketMaker
        );
    }
}
