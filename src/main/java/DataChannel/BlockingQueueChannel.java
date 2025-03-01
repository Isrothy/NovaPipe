package DataChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueChannel implements DataChannel {

    private final BlockingQueue<String> queue;
    private volatile boolean closed;

    public BlockingQueueChannel() {
        this.queue = new LinkedBlockingQueue<>();
        this.closed = false;
    }

    public BlockingQueueChannel(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.closed = false;
    }


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

    @Override
    public void close() throws ChannelException {
        closed = true;
    }
}
