import DataChannel.DataChannel;
import DataChannel.BlockingQueueChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.Normalizer;
import Producer.Producer;
import Producer.QueryGenerator.CoinbaseGenerator;

import java.nio.file.Files;
import java.nio.file.Path;


public class BlockQueueChannelDemo {

    public static void main(String[] args) throws Exception {
        // Create a shared channel for sending messages from the Producer to the Normalizer.
        DataChannel channel = new BlockingQueueChannel();

        // Create a temporary output file to store the normalized JSON messages.
        Path outputFile = Path.of("normalized_output.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // Create and start the Normalizer (runs on its own thread).
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread normalizerThread = new Thread(normalizer);
        normalizerThread.start();

        // Create a Producer that uses the Binance.US QueryGenerator and subscribes to TRADE data for "btcusdt".
        Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, channel);
        Thread producerThread = new Thread(() -> {
            try {
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();

        // Let the pipeline run for a specified duration (e.g., 60 seconds) to collect data.
        System.out.println("Collecting data for 60 seconds...");
        Thread.sleep(60000);

        // After the collection period, close the channel and signal the Normalizer to stop.
        channel.close();
        normalizer.stop();

        // Wait for both threads to finish.
        producerThread.join();
        normalizerThread.join();

        System.out.println("Processing complete. Check the output file at: " + outputFile.toAbsolutePath());
    }
}