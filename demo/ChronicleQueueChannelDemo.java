import DataChannel.DataChannel;
import DataChannel.ChronicleQueueChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

public class ChronicleQueueChannelDemo {

    public static void chronicleQueueChannelDemo()  throws Exception  {
        // Use a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_chronicle.json");
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

    public static void normalizerCrashDemo() throws Exception {
        // Use a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());

        // Use a fixed output file for normalized JSON messages.
        Path outputFile = Path.of("normalized_output_consumer_restart.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

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
        DataChannel consumerChannel1 = new ChronicleQueueChannel(queueDir.toString());
        Normalizer consumer1 = new Normalizer(consumerChannel1, outputFile.toString());
        Thread consumerPhase1 = new Thread(consumer1);
        consumerPhase1.start();
        System.out.println("Consumer Phase 1 started.");
        Thread.sleep(20000);
        System.out.println("Stopping Consumer Phase 1.");
        consumer1.stop();
        consumerPhase1.join();

        // Pause for 5 seconds between consumer restarts.
        Thread.sleep(5000);

        // --- Consumer Side (Phase 2 / Restart) ---
        DataChannel consumerChannel2 = new ChronicleQueueChannel(queueDir.toString());
        Normalizer consumer2 = new Normalizer(consumerChannel2, outputFile.toString());
        Thread consumerPhase2 = new Thread(consumer2);
        consumerPhase2.start();
        System.out.println("Consumer Phase 2 started.");
        // Let the restarted consumer run for another 20 seconds.
        Thread.sleep(20000);
        System.out.println("Stopping Consumer Phase 2.");
        consumer2.stop();
        consumerPhase2.join();

        // Stop the producer by closing its channel.
        channel.close();
        producerThread.interrupt();
        producerThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        normalizerCrashDemo();
    }
}