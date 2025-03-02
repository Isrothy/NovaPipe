import DataChannel.ChronicleQueueChannel;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demonstrates the usage of {@link ChronicleQueueChannel} for persisting market data
 * and enabling recovery in case of consumer failures.
 *
 * <p>
 * The demo consists of two scenarios:
 * <ul>
 *     <li>{@link #chronicleQueueChannelDemo()} - A simple producer-consumer setup using ChronicleQueue.</li>
 *     <li>{@link #normalizerCrashDemo()} - Demonstrates consumer failure and restart while maintaining message integrity.</li>
 * </ul>
 * </p>
 */
public class ChronicleQueueChannelDemo {


    public static void main(String[] args) throws Exception {
//        normalizerCrashDemo();
        chronicleQueueChannelDemo();
    }

    /**
     * Demonstrates a Chronicle Queue-based pipeline where a producer fetches market
     * data from Coinbase and writes it to a persistent queue.
     *
     * <p>
     * The data is then read by a consumer (Normalizer), which processes the messages and writes them
     * to an output file.
     * </p>
     *
     * @throws Exception If any error occurs during execution.
     */
    public static void chronicleQueueChannelDemo() throws Exception {
        // Use a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_chronicle.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // Create a persistent channel based on Chronicle Queue.
        DataChannel channel = new ChronicleQueueChannel(queueDir.toString());

        // --- Consumer Side (Normalizer) ---
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread consumerThread = new Thread(normalizer);
        consumerThread.start();

        // --- Producer Side ---
        Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, channel);
        Thread producerThread = new Thread(() -> {
            try {
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();
    }

    /**
     * Demonstrates a scenario where a consumer (Normalizer) crashes and later restarts,
     * while the producer continues to write data to Chronicle Queue.
     *
     * <p>
     * The consumer initially starts, reads some messages, then stops. After a brief pause,
     * a new consumer instance is created with the same tailer name, allowing it to resume
     * from where the previous instance left off.
     * </p>
     *
     * @throws Exception If any error occurs during execution.
     */
    public static void normalizerCrashDemo() throws Exception {
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