package DataChannel.NetworkChannel;

import DataChannel.DataChannel;
import DataChannel.ChannelException;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class NetworkChannelClient implements DataChannel {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private volatile boolean closed;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_RETRY_DELAY_MS = 2000; // 2 seconds

    public NetworkChannelClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.closed = false;
        connect();
    }

    /**
     * Establish a connection to the server with retry logic.
     */
    private synchronized void connect() throws IOException {
        int attempt = 0;
        while (!closed) {
            try {
                socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Connected to server " + host + ":" + port);
                return;
            } catch (IOException e) {
                attempt++;
                if (attempt > MAX_RETRIES) {
                    throw new IOException("Unable to reconnect after " + MAX_RETRIES + " attempts", e);
                }
                long delay = BASE_RETRY_DELAY_MS * (1L << (attempt - 1)); // Exponential backoff
                System.err.println("Reconnection attempt " + attempt + " failed. Retrying in " + delay + " ms.");
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while trying to reconnect", ie);
                }
            }
        }
    }

    /**
     * Reconnects by closing the current connection and trying to re-establish it.
     */
    private synchronized void reconnect() throws IOException {
        System.err.println("Reconnecting to " + host + ":" + port);
        closeResources();
        connect();
    }

    private synchronized void closeResources() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        socket = null;
        reader = null;
    }

    @Override
    public synchronized void send(String message) throws ChannelException {
        throw new UnsupportedOperationException("Send not supported on a client channel.");
    }

    @Override
    public synchronized String receive() throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new ChannelException("End of stream reached.");
            }
            return line;
        } catch (IOException | ChannelException e) {
            try {
                reconnect();
                String line = reader.readLine();
                if (line == null) {
                    throw new ChannelException("End of stream reached after reconnection.");
                }
                return line;
            } catch (IOException ex) {
                throw new ChannelException("Error receiving message after reconnection.", ex);
            }
        }
    }

    @Override
    public synchronized void close() throws ChannelException {
        closed = true;
        closeResources();
    }
}