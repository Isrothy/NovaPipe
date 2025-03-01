package DataChannel.NetworkChannel;

import DataChannel.ChannelException;
import DataChannel.DataChannel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A network-based implementation of {@link DataChannel} that acts as a server.
 * It listens for incoming client connections and enables bidirectional communication.
 * <p>
 * The server can:
 * <ul>
 *     <li>Accept multiple client connections concurrently.</li>
 *     <li>Broadcast messages to all connected clients.</li>
 *     <li>Receive messages from clients and queue them for retrieval.</li>
 * </ul>
 * <p>
 * Messages received from clients are stored in an internal blocking queue and can be accessed
 * using the {@link #receive()} method. Messages sent using {@link #send(String)} are broadcast
 * to all connected clients.
 * </p>
 */
public class NetworkChannelServer implements DataChannel {
    private final ServerSocket serverSocket;
    // Use a thread-safe list to store client connections.
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    // Blocking queue to store received messages.
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean closed;


    /**
     * Creates a {@code NetworkChannelServer} that listens on the specified port.
     *
     * @param port The port on which the server should listen.
     * @throws IOException If an error occurs while opening the server socket.
     */
    public NetworkChannelServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.closed = false;
        System.out.println("Server listening on port " + port);
        // Start a thread to continuously accept new clients.
        new Thread(this::acceptClients).start();
    }


    /**
     * Continuously accepts incoming client connections.
     * Each client connection is managed by a separate {@link ClientHandler} thread.
     */
    private void acceptClients() {
        while (!closed) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                // Start a thread for reading from this client if needed.
                new Thread(handler).start();
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message The message to be sent.
     * @throws ChannelException If the server is closed or an error occurs while sending.
     */
    @Override
    public void send(String message) throws ChannelException {
        if (closed) {
            throw new ChannelException("Cannot send message; channel is closed.");
        }
        for (ClientHandler client : clients) {
            try {
                client.send(message);
            } catch (IOException e) {
                System.err.println("Error sending message to a client. Disconnecting that client.");
                client.close();
                clients.remove(client);
            }
        }
    }

    /**
     * Receives a message from any connected client.
     * This method blocks until a message is available in the queue.
     *
     * @return The received message.
     * @throws ChannelException If the server is closed or interrupted while waiting for a message.
     */
    @Override
    public String receive() throws ChannelException {
        if (closed && messageQueue.isEmpty()) {
            throw new ChannelException("Server is closed and no more messages are available.");
        }
        try {
            return messageQueue.take(); // Blocks until a message is available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChannelException("Interrupted while waiting for a message.", e);
        }
    }

    /**
     * Closes the server, disconnecting all clients and shutting down resources.
     *
     * @throws ChannelException If an error occurs while closing the server.
     */
    @Override
    public void close() throws ChannelException {
        closed = true;
        try {
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            serverSocket.close();
        } catch (IOException e) {
            throw new ChannelException("Error closing server", e);
        }
    }

    /**
     * A helper class to handle communication with an individual client.
     * <p>
     * Each client is assigned a separate {@code ClientHandler} that:
     * <ul>
     *     <li>Reads incoming messages from the client.</li>
     *     <li>Allows sending messages to the client.</li>
     *     <li>Handles client disconnection.</li>
     * </ul>
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedWriter writer;
        private final BufferedReader reader;
        private volatile boolean closed;

        /**
         * Constructs a {@code ClientHandler} for managing a client connection.
         *
         * @param socket The socket representing the client connection.
         * @throws IOException If an error occurs while setting up input/output streams.
         */
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.closed = false;
        }

        /**
         * Sends a message to the client.
         *
         * @param message The message to send.
         * @throws IOException If an error occurs while sending the message.
         */
        public void send(String message) throws IOException {
            if (!closed) {
                writer.write(message);
                writer.newLine();
                writer.flush();
            }
        }

        /**
         * Closes the client connection and releases resources.
         */
        public void close() {
            closed = true;
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

        /**
         * Reads messages from the client and adds them to the message queue.
         * This method runs in a separate thread for each client.
         */
        @Override
        public void run() {
            try {
                while (!closed) {
                    String line = reader.readLine();
                    if (line == null) {
                        break; // Client disconnected
                    }
                    System.out.println("Received from client: " + line);
                    // Add the received message to the message queue
                    messageQueue.put(line);
                }
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Error reading from client: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted while adding to message queue.");
            } finally {
                close();
            }
        }
    }
}