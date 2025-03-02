package PipelineChannelDemo;

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
 * {@code PipelineChannelDemo} demonstrates a pipeline architecture that chains
 * multiple {@link DataChannel} instances together. In this demo, data is collected
 * from a WebSocket feed via a network channel and then persisted on disk using a
 * ChronicleQueueChannel. The two channels are chained together using a PipelineChannel.
 *
 * <p>
 * The pipeline works as follows:
 * </p>
 * <ol>
 *     <li>
 *         <b>Persistent Storage:</b> A {@link ChronicleQueueChannel} is created to store data on disk.
 *     </li>
 *     <li>
 *         <b>Producer Side:</b> A {@link NetworkChannelServer} is started on a specified port.
 *         A {@link Producer} (using a {@link CoinbaseGenerator}) subscribes to QUOTE data for "BTC-USD"
 *         and sends raw data into the network channel.
 *     </li>
 *     <li>
 *         <b>Network Client Side:</b> A {@link NetworkChannelClient} connects to the producer's port to receive data.
 *     </li>
 *     <li>
 *         <b>Pipeline Channel:</b> A {@link PipelineChannel} is used to forward data from the network client channel to the persistent ChronicleQueueChannel.
 *     </li>
 *     <li>
 *         <b>Consumer Side:</b> A {@link Normalizer} reads data from the pipeline channel, normalizes it, and writes the output in JSONL format to an output file.
 *     </li>
 * </ol>
 *
 * <p>
 * The demo runs for 60 seconds, after which the pipeline is shut down, and all threads are terminated.
 * </p>
 *
 * <p>
 * <b>Note:</b> When using ChronicleQueue, ensure that your JVM is configured with the required command-line options
 * (e.g., <code>--add-opens</code> and <code>--add-exports</code>) as specified in the project documentation.
 * </p>
 */
public class PipelineChannelDemo {


    /**
     * The main entry point for the PipelineChannel demo.
     *
     * @param args command-line arguments (not used)
     * @throws Exception if an error occurs during setup or execution
     */
    public static void main(String[] args) throws Exception {
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