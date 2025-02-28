package Producer.BinanceUs;

import Producer.RawExchangeDataProducer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


class NovaPipeWebSocket implements WebSocket.Listener {

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WebSocket opened.");
        webSocket.request(1);
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        System.out.println("Received message: " + data);
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        System.out.println("Received ping: " + new String(message.array()));
        webSocket.sendPong(message);
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        System.out.println("Received pong: " + new String(message.array()));
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + statusCode + " " + reason);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.out.println("WebSocket error: " + error.getMessage());
    }
}

public class DataProducer implements RawExchangeDataProducer {

    private final String symbol;
    private final MarketDataStreamType type;
    private static int id_counter = 1;

    public DataProducer(String symbol, MarketDataStreamType type) {
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public void produceRawExchangeData() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://stream.binance.us:9443/ws"), new NovaPipeWebSocket());

        wsFuture.thenAccept(webSocket -> {
            System.out.println("WebSocket connection established.");
            String param = symbol + "@" + type.getStreamName();
            String subscribeMessage = String.format("""
                {
                    "method": "SUBSCRIBE",
                    "params": [
                         "%s"
                    ],
                    "id": %d
                }
                """, param, id_counter);
            id_counter += 1;
            webSocket.sendText(subscribeMessage, true);
            webSocket.request(1);
        });

        Thread.currentThread().join();
    }
}
