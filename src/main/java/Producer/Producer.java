package Producer;

import MarketDataQueryType.MarketDataQueryType;
import Producer.QueryGenerator.QueryGenerator;

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

public class Producer {
    private final QueryGenerator gen;
    private final String product;
    private final MarketDataQueryType type;


    public Producer(QueryGenerator gen, String product, MarketDataQueryType type) {
        this.product = product;
        this.type = type;
        this.gen = gen;
    }

    public void produceRawExchangeData() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(gen.getUrl()), new NovaPipeWebSocket());

        wsFuture.thenAccept(webSocket -> {
            System.out.println("WebSocket connection established.");
            var message = gen.generateQueryMessage(product, type);
            webSocket.sendText(message, true);
            webSocket.request(1);
        });

        Thread.currentThread().join();
    }
}
