package Normalizer.PayloadParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import MarketDataType.Quote;
import MarketDataType.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

class CoinbasePayloadParserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CoinbasePayloadParser parser = new CoinbasePayloadParser();

    @Test
    public void testParseQuote() throws Exception {
        String json = """
                {
                  "type": "quote",
                  "sequence": 37475248783,
                  "product_id": "ETH-USD",
                  "price": "1285.22",
                  "open_24h": "1310.79",
                  "volume_24h": "245532.79269678",
                  "low_24h": "1280.52",
                  "high_24h": "1313.8",
                  "volume_30d": "9788783.60117027",
                  "best_bid": "1285.04",
                  "best_bid_size": "0.46688654",
                  "best_ask": "1285.27",
                  "best_ask_size": "1.56637040",
                  "side": "buy",
                  "time": "2022-10-19T23:28:22.061769Z",
                  "trade_id": 370843401,
                  "last_size": "11.4396987"
                }
                """;

        JsonNode root = mapper.readTree(json);
        Quote quote = parser.parseQuote(root);

        assertEquals("coinbase", quote.platform());
        assertEquals(37475248783L, quote.sequence());
        // The parser removes hyphens from the product field.
        assertEquals("ETHUSD", quote.product());
        assertEquals(new BigDecimal("1285.04"), quote.bestBid());
        assertEquals(new BigDecimal("0.46688654"), quote.bestBidSize());
        assertEquals(new BigDecimal("1285.27"), quote.bestAsk());
        assertEquals(new BigDecimal("1.56637040"), quote.bestAskSize());
        assertEquals(new BigDecimal("1285.22"), quote.price());
        // Compare time conversion from ISO string to Instant.
        Instant expectedTime = ZonedDateTime.parse("2022-10-19T23:28:22.061769Z").toInstant();
        assertEquals(expectedTime, quote.time());
        assertEquals("370843401", quote.trade_id());
        assertEquals(new BigDecimal("11.4396987"), quote.last_size());
    }

    @Test
    public void testParseTrade() throws Exception {
        String json = """
                {
                  "type": "match",
                  "trade_id": 10,
                  "sequence": 50,
                  "maker_order_id": "ac928c66-ca53-498f-9c13-a110027a60e8",
                  "taker_order_id": "132fb6ae-456b-4654-b4e0-d681ac05cea1",
                  "time": "2014-11-07T08:19:27.028459Z",
                  "product_id": "BTC-USD",
                  "size": "5.23512",
                  "price": "400.23",
                  "side": "sell"
                }
                """;

        JsonNode root = mapper.readTree(json);
        Trade trade = parser.parseTrade(root);

        assertEquals("coinbase", trade.platform());
        // For Coinbase trade, eventTime might not be set (depending on your implementation), so we expect null.
        assertNull(trade.eventTime());
        // The parser removes hyphens from the product field.
        assertEquals("BTCUSD", trade.product());
        assertEquals(10L, trade.tradeId());
        assertEquals(new BigDecimal("400.23"), trade.price());
        assertEquals(new BigDecimal("5.23512"), trade.size());
        assertEquals("ac928c66-ca53-498f-9c13-a110027a60e8", trade.buyerId());
        assertEquals("132fb6ae-456b-4654-b4e0-d681ac05cea1", trade.sellerId());
        Instant expectedTradeTime = ZonedDateTime.parse("2014-11-07T08:19:27.028459Z").toInstant();
        assertEquals(expectedTradeTime, trade.tradeTime());
        // In Coinbase parser, buyerIsMarketMaker is not set, so expect null.
        assertNull(trade.buyerIsMarketMaker());
    }
}