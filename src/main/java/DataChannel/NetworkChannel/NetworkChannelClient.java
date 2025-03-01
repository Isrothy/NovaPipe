package DataChannel.NetworkChannel;

import DataChannel.ChannelException;
import DataChannel.DataChannel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * The {@code NetworkChannelClient} class implements the {@link DataChannel} interface and
 * acts as a client that connects to a server over a network.
 * It supports receiving messages from the server and reconnecting in case of disconnections.
 */
public class NetworkChannelClient implements DataChannel {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean closed;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_RETRY_DELAY_MS = 2000; // 2 seconds

    /**
     * Creates a {@code NetworkChannelClient} and establishes a connection to the server.
     *
     * @param host The server's hostname or IP address.
     * @param port The port number of the server.
     * @throws IOException if the connection cannot be established.
     */
    public NetworkChannelClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.closed = false;
        connect();
    }


    /**
     * Establishes a connection to the server with an automatic retry mechanism.
     *
     * @throws IOException if the connection fails after the maximum number of retries.
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
     * Reconnects to the server by closing the current connection and attempting to reconnect.
     *
     * @throws IOException if the reconnection fails.
     */
    private synchronized void reconnect() throws IOException {
        System.err.println("Reconnecting to " + host + ":" + port);
        closeResources();
        connect();
    }

    /**
     * Closes all resources associated with the network connection.
     */
    private synchronized void closeResources() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        socket = null;
        reader = null;
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to send.
     * @throws ChannelException if an error occurs while sending the message.
     */
    @Override
    public synchronized void send(String message) throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            try {
                reconnect();
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                throw new ChannelException("Failed to send message after reconnection.", ex);
            }
        }
    }

    /**
     * Receives a message from the server.
     *
     * @return The received message as a {@code String}.
     * @throws ChannelException if an error occurs while receiving the message or if the stream ends.
     */
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

    /**
     * Closes the network channel and releases resources.
     *
     * @throws ChannelException if an error occurs while closing the connection.
     */
    @Override
    public synchronized void close() throws ChannelException {
        closed = true;
        closeResources();
    }
}