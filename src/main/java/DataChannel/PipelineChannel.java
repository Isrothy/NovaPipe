package DataChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code PipelineChannel} chains multiple {@link DataChannel} instances together.
 * <p>
 * The first channel in the pipeline acts as the entry point for messages via {@link #send(String)},
 * while the last channel serves as the exit point where messages are received via {@link #receive()}.
 * Internally, background threads continuously forward data from one channel to the next.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * DataChannel networkChannel = new NetworkChannelClient("localhost", 12345);
 * DataChannel queueChannel = new ChronicleQueueChannel("queue-dir");
 * DataChannel pipeline = new PipelineChannel(networkChannel, queueChannel);
 *
 * pipeline.send("test message");
 * String received = pipeline.receive();
 * }</pre>
 */
public class PipelineChannel implements DataChannel {
    private final DataChannel inputChannel;
    private final DataChannel outputChannel;
    private final List<Thread> forwarderThreads = new ArrayList<>();
    private final List<DataChannel> channels = new ArrayList<>();
    private volatile boolean closed = false;
    private static final Logger logger = LogManager.getLogger(PipelineChannel.class);

    /**
     * Constructs a pipeline from the given {@link DataChannel} instances.
     * The first channel in the list is treated as the input channel, and the last
     * channel is treated as the output channel.
     * <p>
     * A background forwarding thread is started for each adjacent pair of channels
     * to relay messages from one to the next.
     * </p>
     *
     * @param channels The ordered sequence of {@link DataChannel} instances forming the pipeline.
     *                 Must contain at least two channels.
     * @throws IllegalArgumentException if fewer than two channels are provided.
     */
    public PipelineChannel(DataChannel... channels) {
        if (channels == null || channels.length < 2) {
            throw new IllegalArgumentException("At least two channels are required for a pipeline.");
        }
        this.inputChannel = channels[0];
        this.outputChannel = channels[channels.length - 1];
        this.channels.addAll(List.of(channels));

        // For each adjacent pair of channels, start a forwarding thread.
        for (int i = 0; i < channels.length - 1; i++) {
            Thread t = getPipelineThread(channels[i], channels[i + 1]);
            t.start();
            forwarderThreads.add(t);
        }
    }

    /**
     * Creates a thread that continuously forwards messages from the source channel
     * to the destination channel until the pipeline is closed.
     *
     * @param src  The source {@link DataChannel} to read messages from.
     * @param dest The destination {@link DataChannel} to forward messages to.
     * @return A thread that forwards messages between the two channels.
     */
    private Thread getPipelineThread(DataChannel src, DataChannel dest) {
        return new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !closed) {
                    String msg = src.receive();
                    dest.send(msg);
                }
            } catch (ChannelException e) {
                logger.error("Error forwarding message: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sends a message into the pipeline, which is passed to the first channel.
     *
     * @param message The message to send.
     * @throws ChannelException If the pipeline is closed or an error occurs in sending.
     */
    @Override
    public void send(String message) throws ChannelException {
        // Sending to the pipeline sends the message to the first channel.
        if (closed) {
            throw new ChannelException("Pipeline is closed.");
        }
        inputChannel.send(message);
    }

    /**
     * Receives a message from the pipeline, retrieving it from the last channel.
     *
     * @return The received message.
     * @throws ChannelException If the pipeline is closed or an error occurs in receiving.
     */
    @Override
    public String receive() throws ChannelException {
        // Receiving from the pipeline gets the message from the last channel.
        if (closed) {
            throw new ChannelException("Pipeline is closed.");
        }
        return outputChannel.receive();
    }

    /**
     * Closes the pipeline, shutting down all internal channels and forwarding threads.
     * <p>
     * Each {@link DataChannel} in the pipeline is closed, and all background
     * forwarding threads are interrupted.
     * </p>
     *
     * @throws ChannelException If an error occurs during closure.
     */
    @Override
    public void close() throws ChannelException {
        closed = true;
        for (DataChannel channel : channels) {
            channel.close();
        }
        for (Thread t : forwarderThreads) {
            t.interrupt();
        }
    }
}