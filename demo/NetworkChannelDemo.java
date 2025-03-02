import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.BinanceUsQueryGenerator;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demonstrates the usage of {@link NetworkChannelServer} and {@link NetworkChannelClient}
 * in a producer-consumer setting using WebSocket communication.
 * <p>
 * Three demo methods are provided:
 * <ul>
 *     <li>{@code usageDemo()}: Demonstrates a simple producer-consumer interaction.</li>
 *     <li>{@code clientCrashDemo()}: Simulates a client crash and reconnection.</li>
 *     <li>{@code serverCrashDemo()}: Simulates a server crash and recovery.</li>
 * </ul>
 */
public class NetworkChannelDemo {

    public static void main(String[] args) throws Exception {
//        usageDemo();
//        clientCrashDemo();
//        serverCrashDemo();
        multiProducerDemo();
    }

    /**
     * Demonstrates a simple producer-consumer interaction using network channels.
     * <p>
     * The consumer (server) listens for messages and normalizes data, while the producer
     * (client) sends market data via WebSocket.
     *
     * @throws Exception If any error occurs
     */
    public static void usageDemo() throws Exception {
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


    /**
     * Simulates a scenario where the client crashes and reconnects.
     * <p>
     * The producer (server) continuously sends data, and the consumer (client) connects,
     * crashes after 10 seconds, and reconnects after a 5-second delay.
     *
     * @throws Exception If any error occurs
     */
    public static void clientCrashDemo() throws Exception {
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

    /**
     * Simulates a scenario where the server crashes and is restarted.
     * <p>
     * The client initially connects to the server and starts receiving messages.
     * The server crashes after 10 seconds and is restarted after a delay.
     *
     * @throws Exception If any error occurs
     */
    public static void serverCrashDemo() throws Exception {
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

    /**
     * Demonstrates a scenario where a single consumer receives market data
     * from two different producers that fetch data from two separate exchanges.
     *
     * <p>
     * - Producer 1 retrieves market data from Coinbase.<br>
     * - Producer 2 retrieves market data from Binance.US.<br>
     * - Both producers send data via a shared {@link NetworkChannelServer}.<br>
     * - The consumer listens and normalizes data from both producers.
     * </p>
     *
     * @throws Exception If any error occurs
     */
    public static void multiProducerDemo() throws Exception {
        int port = 12345; // Shared port for producers to send data

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_multi_producer.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Consumer Side (Server) ---
        Thread consumerThread = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run(); // Blocks until channel is closed or poison pill is received
            } catch (Exception e) {
                System.err.println("Consumer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerThread.start();

        // Give the server a moment to start.
        Thread.sleep(1000);

        // --- Producer 1: Fetching from Coinbase ---
        Thread producer1Thread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer1 = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, clientChannel);
                producer1.run();
            } catch (Exception e) {
                System.err.println("Producer 1 error (Coinbase): " + e.getMessage());
                e.printStackTrace();
            }
        });
        producer1Thread.start();

        // --- Producer 2: Fetching from Binance.US ---
        Thread producer2Thread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer2 = new Producer(new BinanceUsQueryGenerator(), "btcusdt", MarketDataQueryType.QUOTE, clientChannel);
                producer2.run();
            } catch (Exception e) {
                System.err.println("Producer 2 error (Binance.US): " + e.getMessage());
                e.printStackTrace();
            }
        });
        producer2Thread.start();

        // Let the system run for a while (e.g., 60 seconds).
        System.out.println("Multi-producer pipeline running for 60 seconds...");
        Thread.sleep(60000);

        // Shut down the pipeline:
        producer1Thread.interrupt();
        producer2Thread.interrupt();
        consumerThread.interrupt();

        producer1Thread.join();
        producer2Thread.join();
        consumerThread.join();

        System.out.println("Multi-producer demo complete. Check the output file at: " + outputFile.toAbsolutePath());
    }
}
