package DataChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A {@link DataChannel} implementation using a {@link BlockingQueue}.
 *
 * <p>
 * This class provides a thread-safe, in-memory message queue that blocks on receive
 * operations until a message becomes available.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * DataChannel channel = new BlockingQueueChannel();
 *
 * Thread producer = new Thread(() -> {
 *     try {
 *         channel.send("Hello, World!");
 *     } catch (ChannelException e) {
 *         e.printStackTrace();
 *     }
 * });
 *
 * Thread consumer = new Thread(() -> {
 *     try {
 *         String message = channel.receive();
 *         System.out.println("Received: " + message);
 *     } catch (ChannelException e) {
 *         e.printStackTrace();
 *     }
 * });
 *
 * producer.start();
 * consumer.start();
 *
 * // Close the channel when done
 * channel.close();
 * }</pre>
 *
 * @see DataChannel
 */
public class BlockingQueueChannel implements DataChannel {

    private final BlockingQueue<String> queue;
    private volatile boolean closed;

    /**
     * Creates a {@code BlockingQueueChannel} with an unbounded capacity.
     */
    public BlockingQueueChannel() {
        this.queue = new LinkedBlockingQueue<>();
        this.closed = false;
    }

    /**
     * Creates a {@code BlockingQueueChannel} with a specified capacity limit.
     *
     * @param capacity The maximum number of messages the queue can hold.
     */
    public BlockingQueueChannel(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.closed = false;
    }

    /**
     * Sends a message into the channel.
     * This method blocks if the queue is full (in case of a bounded queue).
     *
     * @param message The message to send.
     * @throws ChannelException If the channel is closed or the thread is interrupted while sending.
     */
    @Override
    public void send(String message) throws ChannelException {
        if (closed) {
            throw new ChannelException("Cannot send message; channel is closed.");
        }
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChannelException("Thread interrupted while sending message.", e);
        }
    }

    /**
     * Receives a message from the channel.
     * This method blocks until a message becomes available.
     *
     * @return The received message.
     * @throws ChannelException If the channel is closed and empty, or the thread is interrupted.
     */
    @Override
    public String receive() throws ChannelException {
        if (closed && queue.isEmpty()) {
            throw new ChannelException("Channel is closed and empty.");
        }
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChannelException("Thread interrupted while receiving message.", e);
        }
    }

    /**
     * Closes the channel, preventing further messages from being sent.
     * Messages already in the queue can still be received.
     */
    @Override
    public void close() throws ChannelException {
        closed = true;
    }
}
