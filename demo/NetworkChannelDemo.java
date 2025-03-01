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

    public static void main(String[] args) throws Exception  {
        int port = 12345;  // Choose an available port

        // Use a fixed output file (instead of a temp file) to store normalized JSON messages.
        Path outputFile = Path.of("normalized_output.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // --- Consumer Side (Server) ---
        // Start the consumer: create a NetworkChannelServer and pass it to the Normalizer.
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
        // Start the producer: create a NetworkChannelClient and pass it to the Producer.
        Thread producerThread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                // Use a real query generator (here CoinbaseGenerator for example) and subscribe to TRADE data.
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.produceRawExchangeData();
            } catch (Exception e) {
                System.err.println("Producer error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        producerThread.start();
    }
}
