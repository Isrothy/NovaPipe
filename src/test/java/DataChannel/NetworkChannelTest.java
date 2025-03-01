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
    public void testNetworkChannelCommunication() throws Exception {
        int port = 12345; // choose an available port

        // Use an ExecutorService to run server and client concurrently.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Start the server in a callable that returns the received message.
        Callable<String> serverTask = () -> {
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                // Wait to receive a message
                return serverChannel.receive();
            }
        };
        Future<String> serverFuture = executor.submit(serverTask);

        // Give the server a moment to start.
        Thread.sleep(1000);

        // Start the client: connect and send a test message.
        Runnable clientTask = () -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                String testMessage = "Hello, Network Channel!";
                clientChannel.send(testMessage);
            } catch (ChannelException | IOException e) {
                throw new RuntimeException(e);
            }
        };
        executor.submit(clientTask);

        // Retrieve the message from the server.
        String receivedMessage = serverFuture.get(5, TimeUnit.SECONDS);
        assertEquals("Hello, Network Channel!", receivedMessage);

        executor.shutdownNow();

    }

    @Test
    public void testNetworkChannelCommunicationMultipleMessages() throws Exception {
        int port = 12345; // Choose an available port
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Server task: accept connection and collect messages until a termination message ("POISON_PILL") is received.
        Callable<List<String>> serverTask = () -> {
            List<String> messages = new ArrayList<>();
            try (DataChannel serverChannel = new NetworkChannelServer(port)) {
                while (true) {
                    String message = serverChannel.receive();
                    if ("POISON_PILL".equals(message)) {
                        System.out.println("Server received termination signal.");
                        break;
                    }
                    System.out.println("Server received: " + message);
                    messages.add(message);
                }
            }
            return messages;
        };

        Future<List<String>> serverFuture = executor.submit(serverTask);

        // Give the server a moment to start.
        Thread.sleep(1000);

        // Client task: connect to the server and send multiple messages.
        Runnable clientTask = () -> {
            try (DataChannel clientChannel = new NetworkChannelClient("localhost", port)) {
                for (int i = 0; i < 5; i++) {
                    String msg = "Test message " + i;
                    clientChannel.send(msg);
                    System.out.println("Client sent: " + msg);
                    Thread.sleep(100); // slight delay between messages
                }
                clientChannel.send("POISON_PILL");
            } catch (ChannelException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        executor.submit(clientTask);

        // Wait for the server to finish and collect messages.
        List<String> receivedMessages = serverFuture.get(5, TimeUnit.SECONDS);

        // Verify that exactly 5 messages were received.
        assertEquals(5, receivedMessages.size(), "Server should receive 5 messages");

        // Verify that messages are as expected.
        for (int i = 0; i < 5; i++) {
            assertEquals("Test message " + i, receivedMessages.get(i));
        }

        executor.shutdownNow();
    }

}
