package DataChannel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChronicleQueueChannelTest {

    @Test
    public void testSendAndReceiveSingleMessage() throws Exception {
        // Create a temporary directory for the queue files.
        Path queueDir = Files.createTempDirectory("chronicleQueueTest");
        try (DataChannel channel = new ChronicleQueueChannel(queueDir.toString())) {
            String message = "Test Message";
            channel.send(message);
            // Since our implementation's receive() returns the next available message,
            // we expect to retrieve the same message.
            String received = channel.receive();
            assertEquals(message, received);
        } finally {
            deleteDirectoryRecursively(queueDir);
        }
    }

    @Test
    public void testPersistenceAfterConsumerCrash() throws Exception {
        // Create a temporary directory for the Chronicle Queue files.
        Path queueDir = Files.createTempDirectory("chronicleQueueTest");
        List<String> messagesSent = new ArrayList<>();
        int messageCount = 5;

        // Phase 1: Write messages to the queue.
        try (DataChannel channel = new ChronicleQueueChannel(queueDir.toString())) {
            for (int i = 0; i < messageCount; i++) {
                String msg = "Message " + i;
                channel.send(msg);
                messagesSent.add(msg);
            }
            // Simulate a consumer crash by closing the channel without reading.
        }

        // Phase 2: Create a new channel instance to read the persisted messages.
        List<String> messagesReceived = new ArrayList<>();
        try (DataChannel channel = new ChronicleQueueChannel(queueDir.toString())) {
            // Since the tailer starts at the beginning, read exactly messageCount messages.
            for (int i = 0; i < messageCount; i++) {
                String msg = channel.receive();
                messagesReceived.add(msg);
            }
        } finally {
            deleteDirectoryRecursively(queueDir);
        }

        // Verify that the messages sent are exactly the ones received.
        assertEquals(messagesSent, messagesReceived);
    }

    @Test
    public void testRestartableTailer() throws Exception {
        // Create a temporary directory for the Chronicle Queue files.
        Path queueDir = Files.createTempDirectory("chronicleQueueTest");
        List<String> messagesSent = new ArrayList<>();
        List<String> messagesReceived = new ArrayList<>();
        int messageCount1 = 5;
        int messageCount2 = 5;

        // Phase 1: Write messages to the queue.
        try (DataChannel channel = new ChronicleQueueChannel(queueDir.toString(), "a")) {
            for (int i = 0; i < messageCount1; i++) {
                String msg = "Message " + i;
                channel.send(msg);
                messagesSent.add(msg);
            }
            for (int i = 0; i < messageCount1; i++) {
                String msg = channel.receive();
                messagesReceived.add(msg);
            }
            for (int i = messageCount1; i < messageCount1 + messageCount2; i++) {
                String msg = "Message " + i;
                channel.send(msg);
                messagesSent.add(msg);
            }
        }

        // Phase 2: Create a new channel instance to read the persisted messages.
        try (DataChannel channel = new ChronicleQueueChannel(queueDir.toString(), "a")) {
            for (int i = 0; i < messageCount2; i++) {
                String msg = channel.receive();
                messagesReceived.add(msg);
            }
        } finally {
            deleteDirectoryRecursively(queueDir);
        }

        // Verify that the messages sent are exactly the ones received.
        assertEquals(messagesSent, messagesReceived);
    }

    /**
     * Recursively deletes a directory and its contents.
     */
    private void deleteDirectoryRecursively(Path path) throws Exception {
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))  // Delete children first
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (Exception e) {
                        System.err.println("Error deleting " + p + ": " + e.getMessage());
                    }
                });
    }
}