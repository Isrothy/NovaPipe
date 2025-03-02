package NetworkChannelDemo;

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
 * The {@code MultiProducerDemo} class demonstrates a scenario where data is produced
 * concurrently by two different producers, each fetching data from a different data source,
 * and sent via a shared network channel to a single consumer.
 *
 * <p>
 * In this demo:
 * </p>
 * <ol>
 *   <li>
 *     <b>Consumer Side (Server):</b> A {@link DataChannel.NetworkChannel.NetworkChannelServer}
 *     is instantiated on a shared port. A {@link Normalizer} runs on this channel to consume
 *     incoming messages and write normalized data in JSONL format to a fixed output file.
 *   </li>
 *   <li>
 *     <b>Producer Side:</b> Two producers are started concurrently:
 *     <ul>
 *       <li>
 *         Producer 1 uses a {@link Producer.QueryGenerator.CoinbaseGenerator} to fetch QUOTE data
 *         for "BTC-USD" from Coinbase.
 *       </li>
 *       <li>
 *         Producer 2 uses a {@link Producer.QueryGenerator.BinanceUsQueryGenerator} to fetch QUOTE data
 *         for "btcusdt" from Binance.US.
 *       </li>
 *     </ul>
 *     Each producer sends its data through a {@link DataChannel.NetworkChannel.NetworkChannelClient}
 *     connected to the shared port.
 *   </li>
 *   <li>
 *     The system runs for a specified duration (60 seconds) after which all threads are interrupted and joined.
 *   </li>
 * </ol>
 *
 * <p>
 * The output of the normalized data is stored in a file in JSONL format, which facilitates easy
 * inspection and integration with other systems.
 * </p>
 */
public class MultiProducerDemo {

    /**
     * The main entry point for the demo.
     *
     * @param args The command-line arguments (not used in this demo).
     * @throws Exception if an error occurs during initialization or execution.
     */
    public static void main(String[] args) throws Exception {
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
