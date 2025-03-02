package Producer;

import DataChannel.ChannelException;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Producer.QueryGenerator.QueryGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The {@code Producer} class is responsible for connecting to a WebSocket-based
 * market data stream, receiving messages, and forwarding them to a specified
 * {@link DataChannel}.
 * <p>
 * It supports different market data providers by utilizing a {@link QueryGenerator}
 * that constructs subscription messages for different exchanges.
 * </p>
 */
public class Producer implements Runnable {
    private final QueryGenerator gen;
    private final String product;
    private final MarketDataQueryType type;
    private final DataChannel channel;
    private boolean firstMessageReceived = false;
    private volatile boolean running = true;
    private static final Logger logger = LogManager.getLogger(Producer.class);

    /**
     * Constructs a {@code Producer} instance that subscribes to market data.
     *
     * @param gen     the {@link QueryGenerator} responsible for generating subscription messages.
     * @param product the financial product (e.g., "BTC-USD") to subscribe to.
     * @param type    the type of market data query (e.g., {@code TRADE}, {@code QUOTE}).
     * @param channel the {@link DataChannel} to which received data will be forwarded.
     */
    public Producer(QueryGenerator gen, String product, MarketDataQueryType type, DataChannel channel) {
        this.product = product;
        this.type = type;
        this.gen = gen;
        this.channel = channel;
    }

    public void stop() {
        running = false;
    }

    /**
     * Runs the WebSocket connection in a separate thread, subscribing to market data
     * and handling incoming messages.
     */
    @Override
    public void run() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                    .buildAsync(URI.create(gen.getUrl()), new NovaPipeWebSocket());

            wsFuture.thenAccept(webSocket -> {
                logger.info("WebSocket connection established.");
                var message = gen.generateQueryMessage(product, type);
                webSocket.sendText(message, true);
                webSocket.request(1);
            });

            while (running && !Thread.currentThread().isInterrupted()) {
                // Sleep a bit (e.g., 1 second) to avoid busy-waiting.
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // reset interruption flag
            logger.error("Producer interrupted.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * WebSocket listener implementation for handling market data messages.
     */
    class NovaPipeWebSocket implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("WebSocket opened.");
            webSocket.request(1);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        /**
         * Handles incoming text messages from the WebSocket.
         * Simply wraps the message in a JSON object and forwards it to the data channel.
         * A tag is added to identify the source of the message.
         *
         * @param webSocket the WebSocket instance.
         * @param data      the received message.
         * @param last      whether this is the last part of a message.
         * @return a completed future.
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            logger.info("Received message: {}", data);
            if (!firstMessageReceived) {
                firstMessageReceived = true;
                logger.info("Ignoring first validation message.");
            } else {
                try {
                    String msg = String.format("""
                            {
                                "tag": "%s@%s",
                                "payload": %s
                            }
                            """, type.toString(), gen.getTag(), data.toString());
                    msg = msg.replaceAll("[\\r\\n]+", "");// Remove newlines. Very important!
                    channel.send(msg);
                } catch (ChannelException e) {
                    throw new RuntimeException(e);
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Handles incoming ping messages. Keep connection alive
         *
         * @param webSocket the WebSocket instance.
         * @param message   the ping message content.
         * @return a completed future.
         */
        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            logger.info("Received ping: {}", new String(message.array()));
            webSocket.sendPong(message);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Handles incoming ping messages. Keep connection alive
         *
         * @param webSocket the WebSocket instance.
         * @param message   the ping message content.
         * @return a completed future.
         */
        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            logger.info("Received pong: {}", new String(message.array()));
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Handles the closure of the WebSocket.
         *
         * @param webSocket  the WebSocket on which the message has been received
         * @param statusCode the status code
         * @param reason     the reason
         */
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logger.info("WebSocket closed: {} {}", statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Handles errors that occur during WebSocket communication.
         *
         * @param webSocket the WebSocket on which the error has occurred
         * @param error     the error
         */
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.info("WebSocket error: {}", error.getMessage());
            throw new RuntimeException("WebSocket encountered an error. Stopping producer.", error);
        }
    }

}
