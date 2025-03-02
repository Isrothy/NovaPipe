package DataChannel;

/**
 * The {@code DataChannel} interface represents a communication channel for sending and receiving messages.
 * It provides an abstraction for various types of messaging mechanisms, such as in-memory queues,
 * persistent message queues, or network-based channels.
 *
 * <p>Implementations of this interface should handle message transmission efficiently and provide
 * necessary synchronization if needed.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * DataChannel channel = new SomeDataChannelImplementation();
 * channel.send("Hello, World!");
 * String message = channel.receive();
 * System.out.println("Received: " + message);
 * channel.close();
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>Implementations should specify whether they support concurrent access and whether additional
 * synchronization is needed when used in multi-threaded environments.</p>
 *
 * <h2>Resource Management:</h2>
 * <p>Since this interface extends {@link AutoCloseable}, it is recommended to use it with
 * a try-with-resources block to ensure proper resource management.</p>
 *
 * @see AutoCloseable
 */
public interface DataChannel extends AutoCloseable {

    /**
     * Sends a message through the channel.
     *
     * @param message The message to send.
     * @throws ChannelException If an error occurs while sending the message or if the channel is closed.
     */
    void send(String message) throws ChannelException;

    /**
     * Receives a message from the channel.
     * <p>
     * Implementations blocks until a message is available, or returns null if the channel is non-blocking.
     * </p>
     *
     * @return The received message
     * @throws ChannelException If an error occurs while receiving the message or if the channel is closed.
     */
    String receive() throws ChannelException;

    /**
     * Closes the channel and releases any underlying resources.
     * <p>
     * Once closed, the channel should not allow sending or receiving messages.
     * </p>
     *
     * @throws ChannelException If an error occurs while closing the channel.
     */
    @Override
    void close() throws ChannelException;

}
