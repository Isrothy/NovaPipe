package DataChannel;

import static org.junit.jupiter.api.Assertions.*;

import DataChannel.ChannelException;
import DataChannel.DataChannel;
import DataChannel.NetworkChannel.NetworkChannelClient;
import DataChannel.NetworkChannel.NetworkChannelServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NetworkChannelTest {

    @Test
    public void testServerSendsMessageToClient() throws Exception {
        int port = 12345; // Choose an available port.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Server task: Open a server channel and send one message.
        Callable<Void> serverTask = () -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Wait briefly to ensure a client connects.
                Thread.sleep(2000);
                serverChannel.send("Hello from server");
                System.out.println("Server sent: Hello from server");
            }
            return null;
        };

        // Client task: Connect to the server and receive the message.
        Callable<String> clientTask = () -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                String received = clientChannel.receive();
                System.out.println("Client received: " + received);
                return received;
            }
        };

        Future<Void> serverFuture = executor.submit(serverTask);
        // Give the server time to set up.
        Thread.sleep(1000);
        Future<String> clientFuture = executor.submit(clientTask);

        // Wait for the client to receive the message.
        String receivedMessage = clientFuture.get(5, TimeUnit.SECONDS);
        assertEquals("Hello from server", receivedMessage);

        executor.shutdownNow();
    }

    @Test
    public void testServerSendsMultipleMessagesToClient() throws Exception {
        int port = 12345; // Choose an available port.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Server task: Open a server channel and send five messages,
        // then send "POISON_PILL" to indicate termination.
        Callable<Void> serverTask = () -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Wait for the client to connect.
                Thread.sleep(2000);
                for (int i = 0; i < 5; i++) {
                    String msg = "Test message " + i;
                    serverChannel.send(msg);
                    System.out.println("Server sent: " + msg);
                    Thread.sleep(100); // slight delay between messages
                }
                serverChannel.send("POISON_PILL");
                System.out.println("Server sent termination signal.");
            }
            return null;
        };

        // Client task: Connect to the server and collect messages until "POISON_PILL" is received.
        Callable<List<String>> clientTask = () -> {
            List<String> messages = new ArrayList<>();
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                while (true) {
                    String msg = clientChannel.receive();
                    if ("POISON_PILL".equals(msg)) {
                        System.out.println("Client received termination signal.");
                        break;
                    }
                    System.out.println("Client received: " + msg);
                    messages.add(msg);
                }
            }
            return messages;
        };

        Future<Void> serverFuture = executor.submit(serverTask);
        // Give the server time to start.
        Thread.sleep(1000);
        Future<List<String>> clientFuture = executor.submit(clientTask);

        // Wait for the client to receive all messages.
        List<String> receivedMessages = clientFuture.get(10, TimeUnit.SECONDS);
        assertEquals(5, receivedMessages.size(), "Client should receive 5 messages");
        for (int i = 0; i < 5; i++) {
            assertEquals("Test message " + i, receivedMessages.get(i));
        }

        executor.shutdownNow();
    }

    @Test
    public void testBroadcastToMultipleClients() throws Exception {
        int port = 12345; // Choose an available port.
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Server task: Create a multi-client server and broadcast several messages.
        Callable<Void> serverTask = () -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Wait briefly so that clients have time to connect.
                Thread.sleep(1000);
                // Broadcast a few messages.
                serverChannel.send("Broadcast message 1");
                System.out.println("Server broadcasted: Broadcast message 1");
                Thread.sleep(1000);
                serverChannel.send("Broadcast message 2");
                System.out.println("Server broadcasted: Broadcast message 2");
                // Send a termination message so clients know to stop.
                serverChannel.send("POISON_PILL");
                System.out.println("Server broadcasted termination signal.");
            }
            return null;
        };

        // Client task: Each client connects, collects broadcast messages until "POISON_PILL" is received.
        Callable<List<String>> clientTask = () -> {
            List<String> messages = new ArrayList<>();
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                while (true) {
                    String msg = clientChannel.receive();
                    if ("POISON_PILL".equals(msg)) {
                        break;
                    }
                    messages.add(msg);
                }
            }
            return messages;
        };

        // Start the server task.
        Future<Void> serverFuture = executor.submit(serverTask);

        // Give the server a moment to start and be ready for connections.
        Thread.sleep(500);

        // Launch multiple client tasks (e.g., 3 clients).
        int clientCount = 3;
        List<Future<List<String>>> clientFutures = new ArrayList<>();
        for (int i = 0; i < clientCount; i++) {
            clientFutures.add(executor.submit(clientTask));
        }

        // Wait for the server task to complete.
        serverFuture.get(10, TimeUnit.SECONDS);

        // Collect results from all clients.
        for (Future<List<String>> future : clientFutures) {
            List<String> received = future.get(10, TimeUnit.SECONDS);
            // Expect each client to receive exactly the two broadcast messages.
            assertEquals(2, received.size(), "Each client should receive 2 broadcast messages");
            assertEquals("Broadcast message 1", received.get(0));
            assertEquals("Broadcast message 2", received.get(1));
        }

        executor.shutdownNow();
    }

    @Test
    public void testClientReconnect() throws Exception {
        int port = 12345;
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Server task: simulate server behavior with two phases.
        // Phase 1: Accept first client, send "Message 1", then close the connection.
        // Phase 2: After a delay, start a new server instance and send "Message 2".
        Callable<Void> serverTask = () -> {
            // Phase 1
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Give the client time to connect.
                Thread.sleep(1000);
                serverChannel.send("Message 1");
                System.out.println("Server sent: Message 1");
            } catch (ChannelException | IOException e) {
                System.err.println("Server phase 1 error: " + e.getMessage());
            }

            // Wait for the client to notice the disconnect and for the port to be released.
            Thread.sleep(1000);

            // Phase 2: Re-establish the server (simulate server restart)
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Give the client time to reconnect.
                Thread.sleep(3000);
                serverChannel.send("Message 2");
                System.out.println("Server sent: Message 2");
            } catch (ChannelException | IOException e) {
                System.err.println("Server phase 2 error: " + e.getMessage());
            }
            return null;
        };

        Future<Void> serverFuture = executor.submit(serverTask);

        // Client task: using ReconnectingNetworkChannelClient, try to receive two messages.
        Callable<String[]> clientTask = () -> {
            String[] messages = new String[2];
            System.out.println("Client connecting to " + port);
            try (DataChannel client = new NetworkChannelClient("localhost", port)) {
                messages[0] = client.receive();
                messages[1] = client.receive();
            }
            return messages;
        };

        Thread.sleep(500);
        Future<String[]> clientFuture = executor.submit(clientTask);

        // Wait for both tasks to complete with generous timeouts.
        serverFuture.get(20, TimeUnit.SECONDS);
        String[] received = clientFuture.get(20, TimeUnit.SECONDS);

        assertNotNull(received, "Received messages should not be null");
        assertEquals("Message 1", received[0], "First message should be 'Message 1'");
        assertEquals("Message 2", received[1], "Second message should be 'Message 2'");

        executor.shutdownNow();
    }
    @Test
    public void testServerReceivesMessagesFromMultipleProducers() throws Exception {
        int port = 12345; // Choose an available port.
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Server task: Receives messages from multiple producers until "POISON_PILL" is received.
        Callable<List<String>> serverTask = () -> {
            List<String> receivedMessages = new ArrayList<>();
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                while (true) {
                    String message = serverChannel.receive();
                    if ("POISON_PILL".equals(message)) {
                        System.out.println("Server received termination signal.");
                        break;
                    }
                    System.out.println("Server received: " + message);
                    receivedMessages.add(message);
                }
            }
            return receivedMessages;
        };

        // Start the server task.
        Future<List<String>> serverFuture = executor.submit(serverTask);

        // Give the server a moment to start.
        Thread.sleep(1000);

        // Producer task: Each producer connects to the server and sends messages.
        Callable<Void> producerTask = () -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                for (int i = 0; i < 3; i++) {
                    String msg = "Producer-" + Thread.currentThread().getId() + " message " + i;
                    clientChannel.send(msg);
                    System.out.println("Producer sent: " + msg);
                    Thread.sleep(100);
                }
            }
            return null;
        };

        // Launch multiple producer tasks (e.g., 3 producers).
        int producerCount = 3;
        List<Future<Void>> producerFutures = new ArrayList<>();
        for (int i = 0; i < producerCount; i++) {
            producerFutures.add(executor.submit(producerTask));
        }

        // Wait for all producers to complete.
        for (Future<Void> future : producerFutures) {
            future.get(5, TimeUnit.SECONDS);
        }

        // Send the termination signal to the server.
        try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
            clientChannel.send("POISON_PILL");
        }

        // Collect and verify the server's received messages.
        List<String> receivedMessages = serverFuture.get(10, TimeUnit.SECONDS);
        assertEquals(producerCount * 3, receivedMessages.size(), "Server should receive all producer messages");

        executor.shutdownNow();
    }
}
