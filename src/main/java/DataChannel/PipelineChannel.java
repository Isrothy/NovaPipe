package DataChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * PipelineChannel chains several DataChannel instances together.
 * The first channel receives data via send() and the last channel provides
 * data via receive(). Internally, forwarding threads are started to read from
 * each channel and send to the next.
 */
public class PipelineChannel implements DataChannel {
    private final DataChannel inputChannel;
    private final DataChannel outputChannel;
    private final List<Thread> forwarderThreads = new ArrayList<>();
    private volatile boolean closed = false;

    /**
     * Constructs a pipeline from the given channels.
     * There must be at least two channels: the first is the pipeline input,
     * and the last is the pipeline output.
     *
     * @param channels The ordered channels in the pipeline.
     */
    public PipelineChannel(DataChannel... channels) {
        if (channels == null || channels.length < 2) {
            throw new IllegalArgumentException("At least two channels are required for a pipeline.");
        }
        this.inputChannel = channels[0];
        this.outputChannel = channels[channels.length - 1];

        // For each adjacent pair of channels, start a forwarding thread.
        for (int i = 0; i < channels.length - 1; i++) {
            Thread t = getPipelineThread(channels[i], channels[i + 1]);
            t.start();
            forwarderThreads.add(t);
        }
    }

    private Thread getPipelineThread(DataChannel src, DataChannel dest) {
        return new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !closed) {
                    String msg = src.receive();
                    dest.send(msg);
                }
            } catch (ChannelException e) {
                System.err.printf("Error forwarding message: %s\n", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void send(String message) throws ChannelException {
        // Sending to the pipeline sends the message to the first channel.
        if (closed) {
            throw new ChannelException("Pipeline is closed.");
        }
        inputChannel.send(message);
    }

    @Override
    public String receive() throws ChannelException {
        // Receiving from the pipeline gets the message from the last channel.
        if (closed) {
            throw new ChannelException("Pipeline is closed.");
        }
        return outputChannel.receive();
    }

    @Override
    public void close() throws ChannelException {
        closed = true;
        inputChannel.close();
        outputChannel.close();
        for (Thread t : forwarderThreads) {
            t.interrupt();
        }
    }
}