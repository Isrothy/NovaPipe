import DataChannel.BlockingQueueChannel;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Demonstrates the usage of {@link BlockingQueueChannel} for passing market data
 * from a {@link Producer} to a {@link Normalizer}.
 *
 * <p>
 * - A shared {@link DataChannel} is used to send data between the producer and the normalizer.<br>
 * - A {@link Producer} fetches market data from Coinbase and sends it to the channel.<br>
 * - A {@link Normalizer} reads from the channel and writes the processed data to a file.<br>
 * - The system runs for 60 seconds before shutting down.<br>
 * </p>
 */
public class BlockQueueChannelDemo {

    /**
     * Main method to run the demo.
     *
     * <p>
     * This method initializes a producer that fetches trade data from Coinbase and
     * sends it through a {@link BlockingQueueChannel} to a normalizer, which processes
     * the messages and writes them to an output file.
     * </p>
     *
     * @param args Command-line arguments (not used)
     * @throws Exception If any error occurs during execution
     */
    public static void main(String[] args) throws Exception {
        // Create a shared channel for sending messages from the Producer to the Normalizer.
        DataChannel channel = new BlockingQueueChannel();

        // Create a temporary output file to store the normalized JSON messages.
        Path outputFile = Path.of("normalized_output.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // Create and start the Normalizer (runs on its own thread).
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread normalizerThread = new Thread(normalizer);
        normalizerThread.start();

        // Create a Producer that uses the Binance.US QueryGenerator and subscribes to TRADE data for "btcusdt".
        Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, channel);
        Thread producerThread = new Thread(() -> {
            try {
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Let the pipeline run for a specified duration (e.g., 60 seconds) to collect data.
        System.out.println("Collecting data for 60 seconds...");
        Thread.sleep(60000);

        // After the collection period, close the channel and signal the Normalizer to stop.
        channel.close();
        normalizer.stop();

        // Wait for both threads to finish.
        producerThread.join();
        normalizerThread.join();

        System.out.println("Processing complete. Check the output file at: " + outputFile.toAbsolutePath());
    }
}