package Demo.NetworkChannelDemo;

import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.BinanceUsQueryGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code NetworkChannelDemo} class demonstrates a network channel-based architecture where a producer and
 * a consumer communicate via a client-server model. In this demo, the consumer side is implemented using a
 * {@link DataChannel.NetworkChannel.NetworkChannelServer} which listens on a specified port, while the producer
 * side is implemented using a {@link DataChannel.NetworkChannel.NetworkChannelClient} that connects to the server.
 *
 * <p>
 * The workflow of this demo is as follows:
 * </p>
 * <ol>
 *   <li>
 *     <b>Output File Setup:</b> A fixed output file is created to store normalized JSON messages in JSON Lines (JSONL)
 *     format. This file serves as the destination for processed data.
 *   </li>
 *   <li>
 *     <b>Consumer Side (Server):</b> A consumer thread is started that creates a NetworkChannelServer on a chosen port.
 *     The consumer uses a {@link Normalizer} to continuously read messages from the channel and write normalized data
 *     to the output file.
 *   </li>
 *   <li>
 *     <b>Producer Side (Client):</b> After a short delay to allow the server to start, a producer thread is launched.
 *     This thread creates a NetworkChannelClient that connects to the server and uses a {@link Producer} (with a
 *     {@link Producer.QueryGenerator.BinanceUsQueryGenerator}) to subscribe to QUOTE data for "btcusdt" and push raw
 *     data into the channel.
 *   </li>
 * </ol>
 *
 * <p>
 * This demo illustrates the basic setup of a network channel where a producer sends data from an external source to
 * a consumer that processes and persists the data. It serves as a foundation for building more complex real-time
 * market data processing pipelines.
 * </p>
 */
public class NetworkChannelDemo {
    /**
     * The main entry point for the demo.
     *
     * @param args The command-line arguments (not used in this demo).
     * @throws Exception if an error occurs during initialization or execution.
     */
    public static void main(String[] args) throws Exception {
        int port = 12345;  // Choose an available port

        // Use a fixed output file (instead of a temp file) to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Consumer Side (Server) ---
        Thread consumerThread = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();  // Run blocking until channel is closed or poison pill is received
            } catch (Exception e) {
                System.err.println("Consumer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerThread.start();

        // Give the server a moment to start.
        Thread.sleep(1000);

        // --- Producer Side (Client) ---
        Thread producerThread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer = new Producer(new BinanceUsQueryGenerator(), "btcusdt", MarketDataQueryType.QUOTE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();
    }
}
