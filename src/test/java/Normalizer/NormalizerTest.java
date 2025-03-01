package Normalizer;

import MarketDataType.Quote;
import MarketDataType.Trade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

import DataChannel.DataChannel;
import DataChannel.BlockingQueueChannel;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

class NormalizerTest {

    @Test
    public void testNormalizerProcessesAndWritesForOneMessage() throws Exception {

        // Create a temporary output file for the Normalizer.
        Path outputFile = Files.createTempFile("normalized_test_output", ".ser");
        System.out.println("Test output file: " + outputFile.toAbsolutePath());

        // Create a channel (assuming QueueChannel is your implementation of DataChannel<String>).
        DataChannel channel = new BlockingQueueChannel();

        // Create and start the Normalizer, which reads from the channel and writes serialized objects.
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread normalizerThread = new Thread(normalizer);
        normalizerThread.start();

        // Create a dummy JSON message that the Normalizer expects.
        // The JSON has a "tag" field in the format "symbol@exchange" and a "payload" field with ticker data.
        String dummyMessage = """
                {
                  "tag": "quote@binance.us",
                  "payload": {
                    "u": 400900217,
                    "s": "BNBUSDT",
                    "b": "25.35190000",
                    "B": "31.21000000",
                    "a": "25.36520000",
                    "A": "40.66000000"
                  }
                }
                """;

        // Send the dummy message into the channel.
        channel.send(dummyMessage);
        // Send the poison pill to signal the Normalizer to stop processing.
        channel.send(Normalizer.POISON_PILL);

        // Wait for the Normalizer thread to finish.
        normalizerThread.join();

        // Read the JSON text from the file.
        List<String> lines = Files.readAllLines(outputFile);
        assertFalse(lines.isEmpty(), "Output file should contain at least one line of JSON");

        String outputJson = lines.get(0);
        System.out.println("Output JSON: " + outputJson);

        // Deserialize the JSON text into a Quote object.
        ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule if needed
        mapper.registerModule(new JavaTimeModule());
        Quote quote = mapper.readValue(outputJson, Quote.class);

        // Assert expected values.
        assertEquals("binance.us", quote.platform());
        assertEquals(400900217L, quote.sequence());
        assertEquals("BNBUSDT", quote.product());
        assertEquals(new java.math.BigDecimal("25.35190000"), quote.bestBid());
        assertEquals(new java.math.BigDecimal("31.21000000"), quote.bestBidSize());
        assertEquals(new java.math.BigDecimal("25.36520000"), quote.bestAsk());
        assertEquals(new java.math.BigDecimal("40.66000000"), quote.bestAskSize());

        // Clean up the temporary file.
        Files.deleteIfExists(outputFile);
    }

    @Test
    public void testNormalizerProcessesAndWritesMultipleMessages() throws Exception {
        // Create a temporary output file for the Normalizer.
        Path outputFile = Files.createTempFile("normalized_test_output_multiple", ".json");
        System.out.println("Test output file: " + outputFile.toAbsolutePath());

        // Create a channel (assuming QueueChannel is your implementation of DataChannel).
        DataChannel channel = new BlockingQueueChannel();

        // Create and start the Normalizer.
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread normalizerThread = new Thread(normalizer);
        normalizerThread.start();

        // Create three dummy JSON messages with slight differences.
        String dummyMessage1 = """
                {
                  "tag": "quote@binance.us",
                  "payload": {
                    "u": 400900217,
                    "s": "BNBUSDT",
                    "b": "25.35190000",
                    "B": "31.21000000",
                    "a": "25.36520000",
                    "A": "40.66000000"
                  }
                }
                """;

        String dummyMessage2 = """
                {
                  "tag": "quote@binance.us",
                  "payload": {
                    "u": 400900218,
                    "s": "BNBUSDT",
                    "b": "26.35190000",
                    "B": "32.21000000",
                    "a": "26.36520000",
                    "A": "41.66000000"
                  }
                }
                """;

        String dummyMessage3 = """
                {
                  "tag": "quote@binance.us",
                  "payload": {
                    "u": 400900219,
                    "s": "BNBUSDT",
                    "b": "27.35190000",
                    "B": "33.21000000",
                    "a": "27.36520000",
                    "A": "42.66000000"
                  }
                }
                """;

        // Send the dummy messages into the channel.
        channel.send(dummyMessage1);
        channel.send(dummyMessage2);
        channel.send(dummyMessage3);
        // Send the poison pill to signal the Normalizer to stop processing.
        channel.send(Normalizer.POISON_PILL);

        // Wait for the Normalizer thread to finish.
        normalizerThread.join();

        // Read the output file as text.
        List<String> lines = Files.readAllLines(outputFile);
        // Expecting three lines (one for each dummy message processed)
        assertEquals(3, lines.size(), "Output file should contain 3 lines of JSON");

        // Use ObjectMapper to deserialize each line into a Quote object.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize and assert for the first message.
        Quote quote1 = mapper.readValue(lines.get(0), Quote.class);
        assertEquals(400900217L, quote1.sequence());
        assertEquals("BNBUSDT", quote1.product());
        assertEquals(new BigDecimal("25.35190000"), quote1.bestBid());
        // Deserialize second message.
        Quote quote2 = mapper.readValue(lines.get(1), Quote.class);
        assertEquals(400900218L, quote2.sequence());
        // Deserialize third message.
        Quote quote3 = mapper.readValue(lines.get(2), Quote.class);
        assertEquals(400900219L, quote3.sequence());

        // Optionally, print the deserialized objects for debugging.
        System.out.println("Deserialized Quote 1: " + quote1);
        System.out.println("Deserialized Quote 2: " + quote2);
        System.out.println("Deserialized Quote 3: " + quote3);

        // Clean up the temporary file.
        Files.deleteIfExists(outputFile);
    }


    @Test
    public void testNormalizerProcessesAndWritesMultipleCoinbaseMessages() throws Exception {
        // Create a temporary output file for the Normalizer (using a .json extension since we're writing JSON text).
        Path outputFile = Files.createTempFile("normalized_coinbase_output", ".json");
        System.out.println("Test output file: " + outputFile.toAbsolutePath());

        // Create a channel (assuming QueueChannel implements DataChannel<String>).
        DataChannel channel = new BlockingQueueChannel();

        // Create and start the Normalizer.
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread normalizerThread = new Thread(normalizer);
        normalizerThread.start();

        // Create a dummy ticker JSON message for Coinbase.
        String dummyTicker = """
                {
                  "tag": "quote@coinbase",
                  "payload": {
                    "type": "ticker",
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
                }
                """;

        // Create a dummy trade JSON message for Coinbase.
        String dummyTrade = """
                {
                  "tag": "trade@coinbase",
                  "payload": {
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
                }
                """;

        // Send the dummy messages.
        channel.send(dummyTicker);
        channel.send(dummyTrade);
        // Send the poison pill to signal the Normalizer to stop processing.
        channel.send(Normalizer.POISON_PILL);

        // Wait for the Normalizer thread to finish.
        normalizerThread.join();

        // Read all lines from the output file.
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size(), "Output file should contain 2 lines of JSON");

        // Set up ObjectMapper to deserialize JSON and handle Java 8 date/time types.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize the first line into a Quote object.
        Quote quote = mapper.readValue(lines.get(0), Quote.class);
        assertEquals("coinbase", quote.platform());
        assertEquals(37475248783L, quote.sequence());
        // Assuming your parser removes hyphens (e.g., "ETH-USD" -> "ETHUSD")
        assertEquals("ETHUSD", quote.product());
        assertEquals(new BigDecimal("1285.22"), quote.price());
        // Additional assertions can be added here based on your implementation.

        // Deserialize the second line into a Trade object.
        Trade trade = mapper.readValue(lines.get(1), Trade.class);
        assertEquals("coinbase", trade.platform());
        assertEquals(10L, trade.tradeId());
        // Again, assuming your parser transforms "BTC-USD" to "BTCUSD"
        assertEquals("BTCUSD", trade.product());
        assertEquals(new BigDecimal("400.23"), trade.price());
        // Additional assertions can be added here.

        // Clean up the temporary file.
        Files.deleteIfExists(outputFile);
    }


}