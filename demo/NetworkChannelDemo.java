import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkChannelDemo {

    //Demo for NetworkChannel
    public static void usageDemo() throws Exception {
        int port = 12345;  // Choose an available port

        // Use a fixed output file (instead of a temp file) to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Producer Side (Server) ---
        Thread producerThread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelServer(port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Give the server a moment to start.
        Thread.sleep(1000);

        // --- Consumer Side (Client) ---
        Thread consumerThread = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelClient("localhost", port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();  // Run blocking until channel is closed or poison pill is received
            } catch (Exception e) {
                System.err.println("Consumer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerThread.start();
    }

    public static void clientCrashDemo() throws Exception {
        int port = 12345;  // Choose an available port

        // Use a fixed output file to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output_reconnect.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Producer Side (Server) ---
        Thread producerThread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelServer(port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Give the producer a moment to start.
        Thread.sleep(1000);

        // --- Consumer Side (Client) Phase 1 ---
        Thread consumerPhase1 = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Normalizer normalizer = new Normalizer(clientChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                System.err.println("Consumer Phase 1 error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerPhase1.start();
        System.out.println("Simulating consumer crash (Phase 1)...");
        Thread.sleep(10000);
        // Simulate crash by interrupting phase 1 thread.
        consumerPhase1.interrupt();
        consumerPhase1.join();

        System.out.println("Consumer Phase 1 stopped.");

        // --- Consumer Side (Client) Phase 2 (Reconnection) ---
        // After a short delay, restart the consumer to simulate reconnection.
        Thread.sleep(5000);
        Thread consumerPhase2 = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Normalizer normalizer = new Normalizer(clientChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                System.err.println("Consumer Phase 2 error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        consumerPhase2.start();
        System.out.println("Simulating consumer crash (Phase 2)...");
        Thread.sleep(10000);


        consumerPhase2.interrupt();
        consumerPhase2.join();

        // Stop producer thread gracefully.
        producerThread.interrupt();
        producerThread.join();

        System.out.println("Demo complete. Check output file at: " + outputFile.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        clientCrashDemo();
    }
}
