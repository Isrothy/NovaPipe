import DataChannel.ChronicleQueueChannel;
import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import DataChannel.PipelineChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demonstrates the usage of {@link PipelineChannel} for chaining multiple data channels.
 * <p>
 * This class includes two primary demonstration methods:
 * <ul>
 *     <li>{@link #pipelineChannelDemo()} - Demonstrates a basic producer-consumer pipeline.</li>
 *     <li>{@link #pipelineChannelCrashDemo()} - Simulates a consumer failure and restart.</li>
 * </ul>
 * </p>
 */
public class PipelineChannelDemo {

    public static void main(String[] args) throws Exception {
//        pipelineChannelCrashDemo();
        pipelineChannelDemo();
    }

    /**
     * Demonstrates a scenario where a consumer (Normalizer) stops and later resumes processing data.
     * <p>
     * The demo simulates a scenario where:
     * <ul>
     *     <li>A producer sends market data via a network channel.</li>
     *     <li>The data passes through a {@link PipelineChannel}, which forwards messages to a {@link ChronicleQueueChannel}.</li>
     *     <li>The consumer (Normalizer) starts reading from the pipeline.</li>
     *     <li>After some time, the consumer stops and later resumes processing.</li>
     * </ul>
     * </p>
     *
     * @throws Exception If any error occurs during execution.
     */
    public static void pipelineChannelCrashDemo() throws Exception {
        int port = 12345;  // Choose an available port

        // Set up a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());
        String tailerName = "tailer"; // A restartable tailer needs a name

        // Set up a fixed output file for normalized JSON messages.
        Path outputFile = Path.of("normalized_output_pause_demo.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Producer Side (Server) ---
        Thread producerThread = new Thread(() -> {
            try (DataChannel networkServerChannel = new NetworkChannelServer(port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, networkServerChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Give the producer a moment to start.
        Thread.sleep(1000);

        DataChannel networkClientChannel1 = new NetworkChannelClient("localhost", port);
        DataChannel chronicleChannel1 = new ChronicleQueueChannel(queueDir.toString(), tailerName);
        DataChannel pipelineChannel1 = new PipelineChannel(networkClientChannel1, chronicleChannel1);

        // --- Consumer Side: Normalizer Phase 1 ---
        Normalizer consumerPhase1 = new Normalizer(pipelineChannel1, outputFile.toString());
        Thread consumerThread1 = new Thread(consumerPhase1);
        consumerThread1.start();
        System.out.println("Consumer Phase 1 started.");

        // Let consumer run for 20 seconds.
        Thread.sleep(10000);
        System.out.println("Pausing Consumer Phase 1...");
        consumerPhase1.stop();
        consumerThread1.join();
        pipelineChannel1.close();
        System.out.println("Consumer Phase 1 stopped.");

        // Simulate a pause before resuming.
        Thread.sleep(5000);

        DataChannel networkClientChannel2 = new NetworkChannelClient("localhost", port);
        DataChannel chronicleChannel2 = new ChronicleQueueChannel(queueDir.toString(), tailerName);
        DataChannel pipelineChannel2 = new PipelineChannel(networkClientChannel2, chronicleChannel2);

        // --- Consumer Side: Normalizer Phase 2 (Resumed) ---
        // Create a new Normalizer instance using the same pipeline channel.
        Normalizer consumerPhase2 = new Normalizer(pipelineChannel2, outputFile.toString());
        Thread consumerThread2 = new Thread(consumerPhase2);
        consumerThread2.start();
        System.out.println("Consumer Phase 2 started (resumed).");

        // Let the resumed consumer run for another 20 seconds.
        Thread.sleep(10000);
        System.out.println("Stopping Consumer Phase 2.");
        consumerPhase2.stop();
        consumerThread2.join();

        // Shutdown: Close channels and interrupt producer.
        pipelineChannel2.close();
        producerThread.interrupt();
        producerThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

    /**
     * Demonstrates a full pipeline where a producer sends market data via a network channel,
     * and a consumer processes the data through a {@link PipelineChannel}.
     * <p>
     * The steps in this demonstration are:
     * <ul>
     *     <li>A producer sends data via a {@link NetworkChannelServer}.</li>
     *     <li>The data is received by a {@link NetworkChannelClient}.</li>
     *     <li>The data is forwarded via a {@link PipelineChannel} to a {@link ChronicleQueueChannel}.</li>
     *     <li>The consumer (Normalizer) reads from the Chronicle queue and processes the data.</li>
     * </ul>
     * </p>
     *
     * @throws Exception If any error occurs during execution.
     */
    public static void pipelineChannelDemo() throws Exception {
        int port = 12345;  // Choose an available port

        // Set up a directory for Chronicle Queue storage.
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        System.out.println("Queue directory: " + queueDir.toAbsolutePath());

        // Set up a fixed output file for normalized messages.
        Path outputFile = Path.of("normalized_output_pipeline.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Persistent Storage with ChronicleQueueChannel ---
        DataChannel chronicleChannel = new ChronicleQueueChannel(queueDir.toString());

        // --- Producer Side (NetworkChannelServer) ---
        Thread producerThread = new Thread(() -> {
            try (DataChannel networkServerChannel = new NetworkChannelServer(port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, networkServerChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Give the producer a moment to start.
        Thread.sleep(1000);

        // --- Network Client Side (Receiver) ---
        DataChannel networkClientChannel = new NetworkChannelClient("localhost", port);

        // --- Pipeline Channel ---
        // The PipelineChannel forwards data from the NetworkChannelClient to the ChronicleQueueChannel.
        DataChannel pipelineChannel = new PipelineChannel(networkClientChannel, chronicleChannel);

        // --- Consumer Side (Normalizer) ---
        Thread consumerThread = new Thread(() -> {
            try {
                Normalizer normalizer = new Normalizer(pipelineChannel, outputFile.toString());
                normalizer.run(); // Blocks until the channel is closed or terminated.
            } catch (Exception e) {
                System.err.println("Normalizer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerThread.start();

        // Let the pipeline run for a specified duration (e.g., 60 seconds).
        System.out.println("Pipeline running for 60 seconds...");
        Thread.sleep(60000);

        pipelineChannel.close();

        producerThread.interrupt();
        consumerThread.interrupt();

        producerThread.join();
        consumerThread.join();

        System.out.println("Pipeline demo complete. Check the output file at: " + outputFile.toAbsolutePath());
    }
}