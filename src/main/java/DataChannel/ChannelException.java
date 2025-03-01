package DataChannel;

/**
 * Exception class representing errors that occur within a {@link DataChannel}.
 *
 * <p>
 * This exception is thrown when there is an issue with message transmission,
 * such as failure to send or receive a message, an invalid state, or a resource-related error.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * try {
 *     channel.send("Hello, World!");
 * } catch (ChannelException e) {
 *     System.err.println("Failed to send message: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see DataChannel
 */
public class ChannelException extends Exception {

    /**
     * Constructs a new {@code ChannelException} with the specified detail message.
     *
     * @param message The detail message describing the error.
     */
    public ChannelException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ChannelException} with the specified detail message and cause.
     *
     * @param message The detail message describing the error.
     * @param cause The cause of the exception, which may be another exception.
     */
    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
