package Normalizer.PayloadParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import MarketDataType.Quote;
import MarketDataType.Trade;

import java.math.BigDecimal;
import java.time.Instant;

class BinanceUsPayloadParserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BinanceUsPayloadParser parser = new BinanceUsPayloadParser();

    @Test
    public void testParseTicker() throws Exception {
        String json = """
                {
                  "u": 400900217,
                  "s": "BNBUSDT",
                  "b": "25.35190000",
                  "B": "31.21000000",
                  "a": "25.36520000",
                  "A": "40.66000000"
                }
                """;

        JsonNode root = mapper.readTree(json);
        Quote quote = parser.parseTicker(root);

        assertEquals("binance.us", quote.platform());
        assertEquals(400900217L, quote.sequence());
        assertEquals("BNBUSDT", quote.product());
        assertEquals(new BigDecimal("25.35190000"), quote.bestBid());
        assertEquals(new BigDecimal("31.21000000"), quote.bestBidSize());
        assertEquals(new BigDecimal("25.36520000"), quote.bestAsk());
        assertEquals(new BigDecimal("40.66000000"), quote.bestAskSize());
        // Additional fields may be null if not set in the parser.
    }

    @Test
    public void testParseTrade() throws Exception {
        String json = """
                {
                  "e": "trade",
                  "E": 1672515782136,
                  "s": "BNBBTC",
                  "t": 12345,
                  "p": "0.001",
                  "q": "100",
                  "b": "88",
                  "a": "50",
                  "T": 1672515782136,
                  "m": true,
                  "M": true
                }
                """;

        JsonNode root = mapper.readTree(json);
        Trade trade = parser.parseTrade(root);

        assertEquals("binance.us", trade.platform());
        assertEquals(Instant.ofEpochMilli(1672515782136L), trade.eventTime());
        assertEquals("BNBBTC", trade.product());
        assertEquals(12345L, trade.tradeId());
        assertEquals(new BigDecimal("0.001"), trade.price());
        assertEquals(new BigDecimal("100"), trade.size());
        assertEquals("88", trade.buyerId());
        assertEquals("50", trade.sellerId());
        assertEquals(Instant.ofEpochMilli(1672515782136L), trade.tradeTime());
        assertTrue(trade.buyerIsMarketMaker());
    }
}