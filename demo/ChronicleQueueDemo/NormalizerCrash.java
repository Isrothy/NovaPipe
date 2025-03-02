package ChronicleQueueDemo;

import DataChannel.ChronicleQueueChannel;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code NormalizerCrash} class demonstrates a scenario where the consumer (Normalizer)
 * experiences a crash and is later restarted while the producer continues to send data.
 * This demo uses a persistent channel based on Chronicle Queue to ensure that data is not lost
 * when the consumer stops and resumes processing.
 *
 * <p>
 * The demo performs the following steps:
 * </p>
 * <ol>
 *   <li>
 *     <b>Producer Side:</b> A persistent {@code ChronicleQueueChannel} is created for the producer.
 *     A {@link Producer} (using a {@link Producer.QueryGenerator.CoinbaseGenerator}) subscribes to QUOTE data for "BTC-USD"
 *     and continuously publishes raw data to the channel.
 *   </li>
 *   <li>
 *     <b>Consumer Phase 1:</b> A consumer is started by creating a {@code ChronicleQueueChannel} with a specified tailer name.
 *     A {@link Normalizer} reads from this channel and processes messages for a short period (10 seconds),
 *     after which it is stopped to simulate a crash.
 *   </li>
 *   <li>
 *     <b>Consumer Phase 2 (Restart):</b> After a brief pause (5 seconds), a new consumer is started on the same tailer.
 *     This simulates the consumer reconnecting and resuming processing of messages from the persistent storage.
 *   </li>
 * </ol>
 *
 * <p>
 * The normalized data is written in JSONL format to a fixed output file. The demo prints the absolute
 * paths of the Chronicle Queue storage directory and the output file, so you can verify the configuration.
 * </p>
 *
 * <p>
 * <b>Important:</b> When using ChronicleQueue on Java 17 and above, ensure that the required JVM options
 * are provided (e.g., {@code --add-opens} and {@code --add-exports}) as specified in the project documentation.
 * </p>
 */
public class NormalizerCrash {

    /**
     * The main entry point for the NormalizerCrash demo.
     * <p>
     * This method executes the following workflow:
     * </p>
     * <ol>
     *   <li>Ensures the existence of a directory for Chronicle Queue storage and creates a fixed output file
     *       for normalized JSON messages.</li>
     *   <li>Starts the Producer by creating a persistent channel and subscribing to market data using the Coinbase query generator.</li>
     *   <li>Starts Consumer Phase 1: A Normalizer is initiated on a ChronicleQueueChannel (with a specified tailer)
     *       to process incoming messages for 10 seconds, simulating a crash when stopped.</li>
     *   <li>After a short pause, starts Consumer Phase 2 to simulate reconnection, where a new Normalizer is started
     *       on the same tailer and processes messages for another 10 seconds.</li>
     *   <li>Finally, the Producer is stopped by closing its channel and interrupting its thread, completing the demo.</li>
     * </ol>
     *
     * @param args command-line arguments (not used)
     * @throws Exception if an error occurs during initialization or execution
     */
    public static void main(String[] args) throws Exception {
        // Use a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());

        // Use a fixed output file for normalized JSON messages.
        Path outputFile = Path.of("normalized_output_consumer_restart.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());
        String tailerName = "tailer";

        // --- Producer Side ---
        // Create a persistent channel for the producer.
        DataChannel channel = new ChronicleQueueChannel(queueDir.toString());
        Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, channel);
        Thread producerThread = new Thread(() -> {
            try {
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();

        // --- Consumer Side (Phase 1) ---
        DataChannel consumerChannel1 = new ChronicleQueueChannel(queueDir.toString(), tailerName);
        Normalizer consumer1 = new Normalizer(consumerChannel1, outputFile.toString());
        Thread consumerPhase1 = new Thread(consumer1);
        consumerPhase1.start();
        System.out.println("Consumer Phase 1 started.");
        Thread.sleep(10000);
        System.out.println("Stopping Consumer Phase 1.");
        consumer1.stop();
        consumerPhase1.join();

        // Pause for 5 seconds between consumer restarts.
        Thread.sleep(5000);

        // --- Consumer Side (Phase 2 / Restart) ---
        DataChannel consumerChannel2 = new ChronicleQueueChannel(queueDir.toString(), tailerName);
        Normalizer consumer2 = new Normalizer(consumerChannel2, outputFile.toString());
        Thread consumerPhase2 = new Thread(consumer2);
        consumerPhase2.start();
        System.out.println("Consumer Phase 2 started.");
        // Let the restarted consumer run for another 20 seconds.
        Thread.sleep(10000);
        System.out.println("Stopping Consumer Phase 2.");
        consumer2.stop();
        consumerPhase2.join();

        // Stop the producer by closing its channel.
        channel.close();
        producerThread.interrupt();
        producerThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }
}
