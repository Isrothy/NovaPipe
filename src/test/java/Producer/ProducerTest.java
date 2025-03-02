package Producer;

import DataChannel.BlockingQueueChannel;
import DataChannel.ChannelException;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Producer.QueryGenerator.BinanceUsQueryGenerator;
import Producer.QueryGenerator.CoinbaseGenerator;
import Producer.QueryGenerator.QueryGenerator;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Producer class.
 * <p>
 * Each test starts one producer (using either Coinbase or Binance.US generator and
 * either TRADE or QUOTE query type) that sends data to a shared BlockingQueueChannel.
 * A consumer thread is started to continuously read messages from the channel and increment
 * a counter. The test then waits for one minute and asserts that at least a few messages have been received.
 * </p>
 */
public class ProducerTest {

    /**
     * Runs a single producer test.
     *
     * @param generator The query generator for the producer.
     * @param product   The product type (e.g. "BTC-USD" or "ETHUSDT").
     * @param type      The market data query type (TRADE or QUOTE).
     * @throws Exception if an error occurs during the test.
     */
    private void runSingleProducerTest(QueryGenerator generator, String product, MarketDataQueryType type, int duration) throws Exception {
        DataChannel channel = new BlockingQueueChannel();
        AtomicInteger receivedCount = new AtomicInteger(0);

        // Start consumer thread to continuously poll messages from the channel.
        Thread consumerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String message = channel.receive();
                    if (message != null) {
                        System.out.println("Consumer received: " + message);
                        receivedCount.incrementAndGet();
                    }
                }
            } catch (ChannelException e) {
                // Channel is closed or interrupted.
            }
        });
        consumerThread.start();

        // Start the producer thread.
        Thread producerThread = new Thread(() -> {
            try {
                Producer producer = new Producer(generator, product, type, channel);
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Let the producer run for the specified time.
        Thread.sleep(duration);

        // Signal threads to stop.
        producerThread.interrupt();
        consumerThread.interrupt();

        producerThread.join();
        consumerThread.join();

        // Close the channel.
        channel.close();

        System.out.println("Total messages received: " + receivedCount.get());
        // Assert that at least 3 messages were received.
        assertTrue(receivedCount.get() >= 3, "No sufficient data received from WebSocket APIs for " + type);
    }

    /**
     * Tests a producer using CoinbaseGenerator with TRADE data.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test
    @DisplayName("Test Coinbase TRADE Producer")
    void testCoinbaseTradeProducer() throws Exception {
        runSingleProducerTest(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, 20000);
    }

    /**
     * Tests a producer using CoinbaseGenerator with QUOTE data.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test
    @DisplayName("Test Coinbase QUOTE Producer")
    void testCoinbaseQuoteProducer() throws Exception {
        runSingleProducerTest(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, 5000);
    }

    /**
     * Tests a producer using BinanceUsQueryGenerator with TRADE data.
     * This needs extra time because data is less frequent.
     * And even with the extra time, it only gets a few messages, so the test may fail.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test
    @DisplayName("Test Binance.US TRADE Producer")
    void testBinanceTradeProducer() throws Exception {
        runSingleProducerTest(new BinanceUsQueryGenerator(), "btcusdt", MarketDataQueryType.TRADE, 180000);
    }

    /**
     * Tests a producer using BinanceUsQueryGenerator with QUOTE data.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test
    @DisplayName("Test Binance.US QUOTE Producer")
    void testBinanceQuoteProducer() throws Exception {
        runSingleProducerTest(new BinanceUsQueryGenerator(), "ethusdt", MarketDataQueryType.QUOTE, 5000);
    }
}