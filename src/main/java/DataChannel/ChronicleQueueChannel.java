package DataChannel;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;

import java.nio.file.Path;

/**
 * {@code ChronicleQueueChannel} is an implementation of {@link DataChannel} that
 * uses Chronicle Queue as a persistent messaging channel.
 * <p>
 * This class enables reliable message passing by writing messages to a
 * file-based queue, which allows messages to be retained even after application restarts.
 * </p>
 *
 * <b>Usage Example:</b>
 * <pre>{@code
 * DataChannel channel = new ChronicleQueueChannel("queue-directory");
 * channel.send("Hello, Chronicle Queue!");
 * String message = channel.receive();
 * System.out.println("Received: " + message);
 * channel.close();
 * }</pre>
 */
public class ChronicleQueueChannel implements DataChannel {
    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final ExcerptTailer tailer;
    private volatile boolean closed = false;

    /**
     * Constructs a {@code ChronicleQueueChannel} for a given queue directory.
     * <p>
     * This constructor creates an instance that manages a single Chronicle Queue
     * where messages can be appended and retrieved sequentially.
     * </p>
     *
     * @param queueDir the directory where Chronicle Queue files will be stored
     */
    public ChronicleQueueChannel(String queueDir) {
        this.queue = ChronicleQueue.singleBuilder(Path.of(queueDir)).build();
        this.appender = queue.createAppender();
        this.tailer = queue.createTailer();
    }

    /**
     * Constructs a {@code ChronicleQueueChannel} with a named tailer.
     * <p>
     * This version allows multiple readers with independent tailers, making it
     * suitable for concurrent consumers reading from the same queue.
     * </p>
     *
     * @param queueDir   the directory where Chronicle Queue files will be stored
     * @param tailerName the name assigned to the tailer instance
     */
    public ChronicleQueueChannel(String queueDir, String tailerName) {
        this.queue = ChronicleQueue.singleBuilder(Path.of(queueDir)).build();
        this.appender = queue.createAppender();
        this.tailer = queue.createTailer(tailerName);
    }

    /**
     * Writes a message to the Chronicle Queue.
     * <p>
     * This method appends a new message to the queue, ensuring persistence.
     * If the channel is closed, an exception is thrown.
     * </p>
     *
     * @param message the message to send
     * @throws ChannelException if the channel is closed or an error occurs while sending
     */
    @Override
    public synchronized void send(String message) throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            // Writes a text message to the queue.
            appender.writeText(message);
        } catch (Exception e) {
            throw new ChannelException("Error sending message", e);
        }
    }

    /**
     * Reads the next available message from the queue.
     * It sends null if the queue is empty.
     *
     * @return The next message from the queue.
     * @throws ChannelException If the channel is closed or an error occurs while reading.
     */
    @Override
    public synchronized String receive() throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            // Reads a text message from the queue; returns null if no message is available.
            return tailer.readText();
        } catch (Exception e) {
            throw new ChannelException("Error receiving message", e);
        }
    }

    /**
     * Closes the channel and releases all associated resources.
     * <p>
     * This method ensures that the Chronicle Queue is properly closed, preventing
     * resource leaks. After calling this method, no further messages can be sent or received.
     * </p>
     *
     * @throws ChannelException if an error occurs while closing the queue
     */
    @Override
    public synchronized void close() throws ChannelException {
        closed = true;
        queue.close();
    }
}