# NovaPipe


> **Important Note:**  
> NovaPipe uses ChronicleQueue for persistent storage. When running this application, you **must** add the following JVM options to your runtime configuration:
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

NovaPipe is a Java-based application designed to ingest and process real-time market data from external sources. The project uses various channel implementations to handle data communication between producers (which subscribe to WebSocket feeds) and consumers (which normalize, serialize, and persist data).

---

## 1. Functionalities and How to Use It

NovaPipe offers the following key functionalities:

- **Real-Time Data Ingestion**: Subscribe to live market data from multiple data sources (e.g., Coinbase, Binance.US).
- **Data Normalization**: Transform raw JSON data into standardized record types.
- **Data Persistence**: Persist normalized data locally using JSONL (JSON Lines) format.
- **Flexible Channel Communication**: Support various channel types for data transfer between components.

---

## 2. Supported Data Sources and Query Types

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
  - Additional fields such as `price`, `open24h`, `volume24h`, `low24h`, `high24h`, `volume30d`, `side`, `time`, `trade_id`, `last_size`.

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

---

## 3. Project Design

NovaPipe follows a modular design that separates concerns into distinct components:

- **Producers**: Subscribe to external WebSocket feeds and push raw data to channels.
- **Consumers (Normalizers)**: Read raw data from channels, normalize it into standardized record types, and serialize it to disk.
- **Channels**: Abstract the communication between producers and consumers. Multiple channel implementations are provided for different use cases.

---

## 4. Supported Channel Types

### 4.1 BlockQueueChannel

- **Description**: A simple in-memory channel using a blocking queue.
- **Use-case**: Lightweight, in-memory communication within a single JVM.

### 4.2 Network Channel

- **Description**: Uses a client-server model to enable communication across machines.
- **Variants**:
  - **Producer as Broadcast Server**: The producer acts as a server broadcasting data to multiple consumers.
  - **Producer as Client**: The producer acts as a client, while consumers operate as servers to support data consumption from various sources.

### 4.3 ChronicleQueueChannel

- **Description**: A persistent channel based on [Chronicle Queue](https://chronicle.software/chronicle-queue/).
- **Advantages**: Data is stored on disk, preventing data loss in the event of consumer crashes and supporting restartability.

### 4.4 PipelineChannel

- **Description**: Chains multiple channels together to form a pipeline.
- **Example**: A `NetworkChannelClient` can be chained with a `ChronicleQueueChannel` so that network data is received and then persisted on disk.

---

## 5. Demo Code

### **Network Channel Demo**

```java
public class NetworkChannelDemo {
    public static void usageDemo() throws Exception {
        int port = 12345;
        Path outputFile = Path.of("normalized_output.json");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        System.out.println("Output file: " + outputFile.toAbsolutePath());

        // Consumer Side (Server)
        Thread consumerThread = new Thread(() -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                Normalizer normalizer = new Normalizer(serverChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        consumerThread.start();
        Thread.sleep(1000);

        // Producer Side (Client)
        Thread producerThread = new Thread(() -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.TRADE, clientChannel);
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();
    }
}
```

### ChronicleQueueChannel Demo

```java
public class ChronicleQueueChannelDemo {
    public static void main(String[] args) throws Exception {
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        Path outputFile = Path.of("normalized_output_chronicle.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        DataChannel channel = new ChronicleQueueChannel(queueDir.toString());

        // Consumer Side: Normalizer
        Normalizer normalizer = new Normalizer(channel, outputFile.toString());
        Thread consumerThread = new Thread(normalizer);
        consumerThread.start();

        // Producer Side
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

```java
public class PipelineChannelDemo {
    public static void main(String[] args) throws Exception {
        int port = 12345;
        Path queueDir = Path.of("queue-data");
        if (!Files.exists(queueDir)) {
            Files.createDirectories(queueDir);
        }
        Path outputFile = Path.of("normalized_output_pipeline.jsonl");
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }

        // Persistent storage channel using Chronicle Queue
        DataChannel chronicleChannel = new ChronicleQueueChannel(queueDir.toString());

        // Producer Side (NetworkChannelServer)
        Thread producerThread = new Thread(() -> {
            try (DataChannel networkServerChannel = new NetworkChannelServer(port)) {
                Producer producer = new Producer(new CoinbaseGenerator(), "BTC-USD", MarketDataQueryType.QUOTE, networkServerChannel);
                producer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producerThread.start();
        Thread.sleep(1000);

        // Network Client Side (Receiver)
        DataChannel networkClientChannel = new NetworkChannelClient("localhost", port);
        // PipelineChannel: forwards data from network client to persistent channel.
        DataChannel pipelineChannel = new PipelineChannel(networkClientChannel, chronicleChannel);

        // Consumer Side (Normalizer)
        Thread consumerThread = new Thread(() -> {
            try {
                Normalizer normalizer = new Normalizer(pipelineChannel, outputFile.toString());
                normalizer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        consumerThread.start();

        // Let the pipeline run for 60 seconds.
        Thread.sleep(60000);
        pipelineChannel.close();
        producerThread.interrupt();
        consumerThread.interrupt();
        producerThread.join();
        consumerThread.join();
    }
} 
```

## 6. Serialized Format: JSONL

NovaPipe uses JSON Lines (JSONL) format for storing normalized data. The advantages of JSONL include:
- Easy to Read: Each line in the file is a valid JSON object.
- Interoperability: JSONL is supported by many languages and tools.
- Stream Processing: Ideal for log processing and incremental reading.