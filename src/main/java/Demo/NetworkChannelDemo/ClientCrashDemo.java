package Demo.NetworkChannelDemo;

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
 * The {@code ClientCrashDemo} class demonstrates a scenario where the producer (running as a client)
 * experiences a crash and later reconnects, while the consumer (running as a server) continuously
 * receives and processes data. In this demo, the consumer side is implemented using a
 * {@link DataChannel.NetworkChannel.NetworkChannelServer} that listens on a specified port and
 * runs a {@link Normalizer} to write normalized JSON Lines (JSONL) data to an output file. The
 * producer side is implemented as a client via {@link DataChannel.NetworkChannel.NetworkChannelClient}
 * that connects to the consumer's server port and runs a {@link Producer} (using a
 * {@link Producer.QueryGenerator.CoinbaseGenerator}) to subscribe to TRADE data for "BTC-USD".
 *
 * <p>
 * The demo workflow is as follows:
 * </p>
 * <ol>
 *   <li>
 *     <b>Setup:</b> Create an output file for normalized JSON messages.
 *   </li>
 *   <li>
 *     <b>Consumer Side (Server):</b> A consumer thread is started that creates a
 *     {@link DataChannel.NetworkChannel.NetworkChannelServer} on a chosen port. A {@link Normalizer}
 *     is then instantiated on this channel to continuously process incoming messages.
 *   </li>
 *   <li>
 *     <b>Producer Side (Client) Phase 1:</b> A producer thread is started that creates a
 *     {@link DataChannel.NetworkChannel.NetworkChannelClient} to connect to the consumer's server,
 *     and a {@link Producer} subscribes to TRADE data from "BTC-USD" and sends raw data to the consumer.
 *   </li>
 *   <li>
 *     After 10 seconds, the producer (client) thread is interrupted to simulate a crash.
 *   </li>
 *   <li>
 *     <b>Producer Side (Client) Phase 2 (Reconnection):</b> Following a short delay, a new producer
 *     thread is started to simulate reconnection, allowing data transmission to resume.
 *   </li>
 *   <li>
 *     Finally, the consumer (server) thread is interrupted to end the demo gracefully.
 *   </li>
 * </ol>
 *
 * <p>
 * The normalized output is stored in a JSONL file (e.g., "normalized_output_reconnect.jsonl"), which is
 * chosen for its human-readability and ease of integration with other systems.
 * </p>
 */
public class ClientCrashDemo {

    /**
     * The main entry point for the demo.
     *
     * @param args The command-line arguments (not used in this demo).
     * @throws Exception if an error occurs during initialization or execution.
     */
    public static void main(String[] args) throws Exception {
        int port = 12345;  // Choose an available port

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_reconnect.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Producer Side (Server) ---
        Thread serverThread = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Give the producer a moment to start.
        Thread.sleep(1000);

        // --- Consumer Side (Client) Phase 1 ---
        Thread clientThread1 = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Consumer Phase 1 error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        clientThread1.start();
        System.out.println("Simulating consumer crash (Phase 1)...");
        Thread.sleep(10000);
        // Simulate crash by interrupting phase 1 thread.
        clientThread1.interrupt();
        clientThread1.join();

        System.out.println("Consumer Phase 1 stopped.");

        // --- Consumer Side (Client) Phase 2 (Reconnection) ---
        // After a short delay, restart the consumer to simulate reconnection.
        Thread.sleep(5000);
        Thread clientThread2 = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Consumer Phase 2 error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        clientThread2.start();
        System.out.println("Simulating consumer crash (Phase 2)...");
        Thread.sleep(10000);


        clientThread2.interrupt();
        clientThread2.join();

        // Stop producer thread gracefully.
        serverThread.interrupt();
        serverThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

}
