package Demo.PipelineChannelDemo;

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
 * The {@code pipelineChannelCrashDemo} class demonstrates a pipeline architecture where the consumer
 * (Normalizer) is temporarily paused and later resumed while the producer continues to send data.
 * This demo illustrates how to use a chained channel (a {@link PipelineChannel}) that connects a
 * network client channel to a persistent ChronicleQueueChannel, allowing messages to be stored on disk
 * even when the consumer is not actively processing data.
 *
 * <p>
 * The demo workflow is as follows:
 * </p>
 * <ol>
 *   <li>
 *     <b>Producer Setup (Server):</b> A producer is started using a {@link NetworkChannelServer} on a specified port.
 *         The producer uses a {@link Producer.QueryGenerator.CoinbaseGenerator} to subscribe to QUOTE data for "BTC-USD" and sends data
 *         into the network channel.
 *   </li>
 *   <li>
 *     <b>Consumer Phase 1 (Initial Processing):</b> The consumer connects via a {@link NetworkChannelClient} and chains it with
 *         a {@link ChronicleQueueChannel} (with a restartable tailer) using a {@link PipelineChannel}. A {@link Normalizer} reads data
 *         from this pipeline and writes normalized JSON Lines (JSONL) to an output file. This phase runs for a specified duration
 *         (e.g., 10 seconds) and then stops.
 *   </li>
 *   <li>
 *     <b>Pause:</b> After Consumer Phase 1 stops, the demo simulates a pause (e.g., 5 seconds) during which data continues to be
 *         collected and stored persistently by the ChronicleQueueChannel.
 *   </li>
 *   <li>
 *     <b>Consumer Phase 2 (Resumed Processing):</b> A new consumer instance is started on the same tailer, reconnecting to the
 *         persistent channel to resume processing and writing data to the output file.
 *   </li>
 *   <li>
 *     Finally, the pipeline is shut down, and the producer is interrupted gracefully.
 *   </li>
 * </ol>
 *
 *
 * <p>
 * <b>Important:</b> When using ChronicleQueue on Java 17 and above, ensure that the required JVM options (e.g.,
 * {@code --add-opens} and {@code --add-exports}) are set appropriately as described in the project documentation.
 * </p>
 */
public class PipelineChannelCrashDemo {

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
    public static void main(String[] args) throws Exception {
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

        DataChannel networkClientChannel = new NetworkChannelClient("localhost", port);
        DataChannel chronicleChannel = new ChronicleQueueChannel(queueDir.toString(), tailerName);
        DataChannel pipelineChannel = new PipelineChannel(networkClientChannel, chronicleChannel);

        // --- Consumer Side: Normalizer Phase 1 ---
        Normalizer consumerPhase1 = new Normalizer(pipelineChannel, outputFile.toString());
        Thread consumerThread1 = new Thread(consumerPhase1);
        consumerThread1.start();
        System.out.println("Consumer Phase 1 started.");

        // Let consumer run for 10 seconds.
        Thread.sleep(10000);
        System.out.println("Pausing Consumer Phase 1...");
        consumerPhase1.stop();
        consumerThread1.join();
        System.out.println("Consumer Phase 1 stopped.");

        // Simulate a pause before resuming.
        Thread.sleep(5000);

        // --- Consumer Side: Normalizer Phase 2 (Resumed) ---
        // Create a new Normalizer instance using the same pipeline channel.
        Normalizer consumerPhase2 = new Normalizer(pipelineChannel, outputFile.toString());
        Thread consumerThread2 = new Thread(consumerPhase2);
        consumerThread2.start();
        System.out.println("Consumer Phase 2 started (resumed).");

        // Let the resumed consumer run for another 10 seconds.
        Thread.sleep(10000);
        System.out.println("Stopping Consumer Phase 2.");
        consumerPhase2.stop();
        consumerThread2.join();
        pipelineChannel.close();

        // Shutdown: Close channels and interrupt producer.
        producerThread.interrupt();
        producerThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

}
