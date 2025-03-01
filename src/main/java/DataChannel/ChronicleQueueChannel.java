package DataChannel;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;

import java.nio.file.Path;

public class ChronicleQueueChannel implements DataChannel {
    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final ExcerptTailer tailer;
    private volatile boolean closed = false;

    /**
     * Creates a persistent channel using Chronicle Queue.
     *
     * @param queueDir the directory where the queue files will be stored
     */
    public ChronicleQueueChannel(String queueDir) {
        this.queue = ChronicleQueue.singleBuilder(Path.of(queueDir)).build();
        this.appender = queue.createAppender();
        this.tailer = queue.createTailer();
    }

    /**
     * Creates a persistent channel with restartable tailer
     *
     * @param queueDir the directory where the queue files will be stored
     * @param tailerName the name of the tailer
     */
    public ChronicleQueueChannel(String queueDir, String tailerName) {
        this.queue = ChronicleQueue.singleBuilder(Path.of(queueDir)).build();
        this.appender = queue.createAppender();
        this.tailer = queue.createTailer(tailerName);
    }

    /**
     * Writes the message to the persistent queue.
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
     * Reads the next available message from the persistent queue.
     * Note: If no message is available, this implementation returns null.
     */
    @Override
    public synchronized String receive() throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            // Reads a text message from the queue; returns null if no message is available.
            String msg = tailer.readText();
            return msg;
        } catch (Exception e) {
            throw new ChannelException("Error receiving message", e);
        }
    }

    /**
     * Closes the underlying Chronicle Queue, releasing all resources.
     */
    @Override
    public synchronized void close() throws ChannelException {
        closed = true;
        queue.close();
    }
}