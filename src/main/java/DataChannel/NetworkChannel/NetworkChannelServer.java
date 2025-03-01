package DataChannel.NetworkChannel;

import DataChannel.DataChannel;
import DataChannel.ChannelException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkChannelServer implements DataChannel {
    private final ServerSocket serverSocket;
    // Use a thread-safe list to store client connections.
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    public NetworkChannelServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.closed = false;
        System.out.println("Server listening on port " + port);
        // Start a thread to continuously accept new clients.
        new Thread(this::acceptClients).start();
    }

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
     * Broadcasts the message to all connected clients.
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
     * This is a broadcast server, so receiving is not supported.
     */
    @Override
    public String receive() throws ChannelException {
        throw new UnsupportedOperationException("Receive not supported on a broadcast server channel.");
    }

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
     * A helper class to handle individual client connections.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedWriter writer;
        private final BufferedReader reader;
        private volatile boolean closed;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.closed = false;
        }

        public void send(String message) throws IOException {
            if (!closed) {
                writer.write(message);
                writer.newLine();
                writer.flush();
            }
        }

        public void close() {
            closed = true;
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            // Optionally, handle incoming messages from the client.
            // For a broadcast server, you might not need to read from clients.
            try {
                while (!closed) {
                    String line = reader.readLine();
                    if (line == null) {
                        break; // Client disconnected
                    }
                    System.out.println("Received from client: " + line);
                }
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Error reading from client: " + e.getMessage());
                }
            } finally {
                close();
            }
        }
    }
}