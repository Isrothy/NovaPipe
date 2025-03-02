package NetworkChannelDemo;

import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * The {@code ServerCrashDemo} class demonstrates a scenario where the consumer side (acting as the server)
 * crashes and is later restarted, while the producer side (acting as the client) continuously sends messages.
 *
 * <p>
 * In this demo, the roles are defined as follows:
 * </p>
 * <ul>
 *   <li>
 *     <b>Consumer Side (Server):</b> The consumer runs on the server. It creates a
 *     {@link DataChannel.NetworkChannel.NetworkChannelServer} that listens on a specified port.
 *     A {@link Normalizer} is then instantiated on this channel to process incoming messages and write
 *     normalized JSON Lines (JSONL) data to an output file.
 *   </li>
 *   <li>
 *     <b>Producer Side (Client):</b> The producer runs as a client. It creates a
 *     {@link DataChannel.NetworkChannel.NetworkChannelClient} that connects to the server's port.
 *     A {@link Producer} (using a {@link Producer.QueryGenerator.CoinbaseGenerator}) subscribes to TRADE
 *     data for "BTC-USD" and sends data to the consumer.
 *   </li>
 * </ul>
 *
 * <p>
 * The demo workflow is as follows:
 * </p>
 * <ol>
 *   <li>
 *     Start the consumer thread (server) that listens for incoming messages and processes them.
 *   </li>
 *   <li>
 *     Start the producer thread (client) that connects to the consumer's server and sends messages.
 *   </li>
 *   <li>
 *     After a fixed time period (10 seconds), the consumer (server) is interrupted to simulate a crash.
 *   </li>
 *   <li>
 *     After a delay, the consumer is restarted on the same port, simulating reconnection.
 *   </li>
 * </ol>
 */
public class ServerCrashDemo {

    /**
     * The main entry point for the demo.
     *
     * @param args The command-line arguments (not used in this demo).
     * @throws Exception if an error occurs during initialization or execution.
     */
    public static void main(String[] args) throws Exception {
        int port = 12345;  // Choose an available port

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_server_crash.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Consumer Side (Client) Phase 1 ---
        // Start a consumer thread that connects to the server and receives messages.
        Thread consumer = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Consumer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumer.start();

        // --- Producer Side (Server) Phase 1 ---
        Thread producerPhase1 = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                System.err.println("Producer Phase 1 error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerPhase1.start();

        Thread.sleep(10000);
        producerPhase1.interrupt();
        producerPhase1.join();
        System.out.println("Phase 1 complete. (Server crashed)");

        // --- Producer Side (Server) Phase 1 ---
        Thread producerPhase2 = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                System.err.println("Producer Phase 1 error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        Thread.sleep(10000);
        System.out.println("Simulating phrase 2...");
        producerPhase2.start();


        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

}
