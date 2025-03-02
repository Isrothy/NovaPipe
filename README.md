# NovaPipe

> [!IMPORTANT]
> NovaPipe uses ChronicleQueue for persistent storage. When running this application, you **must** add the following JVM
> options to your runtime configuration:
> 
> ```
> --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
> --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
> --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
> --add-exports java.base/sun.nio.ch=ALL-UNNAMED \
> --add-exports jdk.unsupported/sun.misc=ALL-UNNAMED \
> --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
> --add-opens jdk.compiler/com.sun.tools.javac=ALL-UNNAMED \
> --add-opens java.base/java.lang=ALL-UNNAMED \
> --add-opens java.base/java.io=ALL-UNNAMED \
> --add-opens java.base/java.util=ALL-UNNAMED
> ```
> These options are necessary for ChronicleQueue to work properly on Java 17 and above.

NovaPipe is a Java-based application designed to ingest and process real-time market data from external sources. The
project uses various channel implementations to handle data communication between producers (which subscribe to
WebSocket feeds) and consumers (which normalize, serialize, and persist data).

---

## Functionalities

NovaPipe offers the following key functionalities:

- **Real-Time Data Ingestion**: Subscribe to live market data from multiple data sources (e.g., Coinbase, Binance.US).
- **Data Normalization**: Transform raw JSON data into standardized record types.
- **Data Persistence**: Persist normalized data locally using JSONL (JSON Lines) format.
- **Flexible Channel Communication**: Support various channel types for data transfer between components.

## Getting Started

The project is built using Maven. To run the demo, execute the following commands:

```bash
source env.sh # load environment variables
mvn clean install -DskipTests
mvn exec:java -Dexec.mainClass="Demo.BlockQueueChannelDemo"
```

More demo code can be found in the ``src/main/java/Demo`` directory.

## Supported Data Sources and Query Types

### Data Sources

- **Coinbase**: Uses Coinbase's WebSocket feed.
- **Binance.US**: Uses Binance.US's WebSocket feed.

### Query Types

- **TRADE**: Retrieves trade data.
- **QUOTE**: Retrieves quote data (ticker and order book information).

### Record Types for Query Results

- **Quote**:
    - `platform` (String): Data source (e.g., "coinbase", "binance.us").
    - `sequence` (long): Update sequence number.
    - `product` (String): Market product (e.g., "BTC-USD").
    - `bestBid`, `bestBidSize`, `bestAsk`, `bestAskSize` (BigDecimal): Best bid/ask price and quantities.
    - Additional fields such as `price`, `open24h`, `volume24h`, `low24h`, `high24h`, `volume30d`, `side`, `time`,
      `trade_id`, `last_size`.

- **Trade**:
    - `platform` (String): Data source.
    - `eventTime` (Instant): Time of the event.
    - `product` (String): Trading pair.
    - `tradeId` (Long): Unique trade identifier.
    - `price`, `size` (BigDecimal): Trade price and quantity.
    - `buyerId`, `sellerId` (String): Buyer and seller order IDs.
    - `side` (String): Indicates the trade direction.
    - `tradeTime` (Instant): Time the trade was executed.
    - `buyerIsMarketMaker` (Boolean): Indicates if the buyer is the market maker.

## Project Design

NovaPipe follows a modular design that separates concerns into distinct components:

- **Producers**: Subscribe to external WebSocket feeds and push raw data to channels.
- **Consumers (Normalizers)**: Read raw data from channels, normalize it into standardized record types, and serialize
  it to disk.
- **Channels**: Abstract the communication between producers and consumers. Multiple channel implementations are
  provided for different use cases.

---

## Supported Channel Types

### BlockQueueChannel

- **Description**: A simple in-memory channel using a blocking queue.
- **Use-case**: Lightweight, in-memory communication within a single JVM.

### Network Channel

- **Description**: Uses a client-server model to enable communication across machines.
- **Variants**:
    - **Producer as Broadcast Server**: The producer acts as a server broadcasting data to multiple consumers.
    - **Producer as Client**: The producer acts as a client, while consumers operate as servers to support data
      consumption from various sources.

### ChronicleQueueChannel

- **Description**: A persistent channel based on [Chronicle Queue](https://chronicle.software/chronicle-queue/).
- **Advantages**: Data is stored on disk, preventing data loss in the event of consumer crashes and supporting
  restartability.

### PipelineChannel

- **Description**: Chains multiple channels together to form a pipeline.
- **Example**: A `NetworkChannelClient` can be chained with a `ChronicleQueueChannel` so that network data is received
  and then persisted on disk.

## Robustness and Fault Tolerance

NovaPipe is designed with robustness in mind using a client-server architecture that ensures the system remains
resilient even when individual components fail. Here are some key design features that contribute to its fault
tolerance:

- **Client-Server Pattern:**  
  The system is built on a client-server model where producers and consumers communicate via well-defined channels. This
  separation ensures that if one component (client or server) crashes, it does not directly affect the others.

- **Flexible Connectivity:**  
  The architecture allows a single server to connect to multiple clients. This supports both:
    - A **broadcast pattern**, where the producer (acting as a server) pushes data to many consumers.
    - A **multiproducer-to-consumer pattern**, where several producers (acting as clients) send data to a single
      consumer (acting as a server).

- **Persistent Storage with ChronicleQueue:**  
  Unnormalized data is persisted on disk using ChronicleQueue. This persistent storage mechanism means that even if a
  component crashes, the data already received is not lost. The system can easily reboot and resume processing from
  where it left off.

- **Resilient Pipeline:**  
  The system supports chaining of channels (using the PipelineChannel) so that data can be seamlessly passed through
  multiple stages. This design minimizes the risk of data loss if one channel in the pipeline fails.

For practical examples of these robustness features, please refer to the demos in the `src/main/java/Demo` directory.
These demos
illustrate scenarios such as:

- Reconnection after a client or server crash.
- Multiple producers sending data concurrently.
- Recovery from unexpected shutdowns using persisted data.

## Demo Code

### **BlockQueueChannel Demo**

This demo uses an in-memory `BlockingQueueChannel`. The producer pushes data into the channel, and the consumer (
Normalizer) reads, normalizes, and writes the data to a file in JSONL format. The system runs for 60 seconds before
shutting down.

```java
public class BlockQueueChannelDemo {
    public static void main(String[] args) throws Exception {
        // Create a shared channel for sending messages from the Producer to the Normalizer.
        DataChannel channel = new BlockingQueueChannel();

        // Create a temporary output file to store the normalized JSON messages.
        Path outputFile = Path.of("normalized_output.jsonl");
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
```

### **Network Channel Demo**

This demo demonstrates a network channel setup where the consumer acts as a server using `NetworkChannelServer` and the
producer acts as a client using `NetworkChannelClient`. The producer subscribes to market data and sends it to the
consumer, which normalizes and persists the data.

```java
public class NetworkChannelDemo {
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
```

### ChronicleQueueChannel Demo

In this demo, a persistent channel (`ChronicleQueueChannel`) is used to store data on disk. This ensures data is
retained
even if the consumer crashes. Both the producer and consumer share this persistent channel, and normalized data is
written to a `JSONL` file.

```java
public class ChronicleQueueChannelDemo {
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
```

### PipelineChannel Demo

This demo uses a `PipelineChannel` to chain a network client channel with a persistent `ChronicleQueueChannel`. Data is
received over the network from the producer and then forwarded to the persistent channel before being consumed and
normalized. This setup demonstrates how to build a multi-stage data pipeline.

```java
public class PipelineChannelDemo {
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
```

## Serialized Format: `JSONL`

NovaPipe uses JSON Lines (`JSONL`) format for storing normalized data. The advantages of JSONL include:

- **Easy to Read**: Each line in the file is a valid JSON object.
- **Interoperability**: JSONL is supported by many languages and tools.
- **Stream Processing**: Ideal for log processing and incremental reading.