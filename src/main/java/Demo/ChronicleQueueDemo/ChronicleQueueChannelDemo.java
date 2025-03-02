package Demo.ChronicleQueueDemo;

import DataChannel.ChronicleQueueChannel;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * The {@code ChronicleQueueChannelDemo} class demonstrates the use of the {@link ChronicleQueueChannel}
 * as a persistent channel for market data. This demo sets up a persistent channel using Chronicle Queue,
 * starts a consumer (the {@link Normalizer}) to process and serialize incoming data into a JSON Lines file,
 * and starts a producer to subscribe to market data (using the {@link CoinbaseGenerator}) which pushes data
 * into the channel.
 * <p>
 * The demo uses the following components:
 * <ul>
 *     <li><b>ChronicleQueueChannel</b>: A persistent channel implementation that stores data on disk.</li>
 *     <li><b>Normalizer</b>: A consumer that reads raw messages from the channel, normalizes the data,
 *         and writes the output in JSONL format.</li>
 *     <li><b>Producer</b>: A producer that connects to a data source (via WebSocket) and pushes raw data
 *         to the channel.</li>
 * </ul>
 * <p>
 * To run this demo, ensure that the directory for Chronicle Queue storage ("queue-data") and the output file
 * ("normalized_output_chronicle.jsonl") are available. The demo will print the absolute paths of these resources.
 * </p>
 * <p>
 * Note: When using ChronicleQueue on Java 11 and above, you must include the required JVM options (e.g.,
 * {@code --add-opens} and {@code --add-exports}) as specified in the project documentation.
 * </p>
 */
public class ChronicleQueueChannelDemo {


    /**
     * The main entry point for the ChronicleQueueChannel demo.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Creates (or ensures the existence of) a directory for Chronicle Queue storage.</li>
     *     <li>Creates a fixed output file to store normalized JSON messages in JSONL format.</li>
     *     <li>Initializes a persistent {@link ChronicleQueueChannel} based on the storage directory.</li>
     *     <li>Starts a consumer thread using the {@link Normalizer} to read from the channel and write normalized data to the output file.</li>
     *     <li>Starts a producer thread using a {@link CoinbaseGenerator} to subscribe to QUOTE data for "BTC-USD"
     *         and push data into the channel.</li>
     * </ol>
     *
     * @param args Command-line arguments (not used).
     * @throws Exception if an error occurs during setup or execution.
     */
    public static void main(String[] args) throws Exception {
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


}