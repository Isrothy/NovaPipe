package DataChannel.NetworkChannel;

import DataChannel.DataChannel;
import DataChannel.ChannelException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkChannelServer implements DataChannel {
    private final ServerSocket serverSocket;
    private final Socket clientSocket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private volatile boolean closed;

    public NetworkChannelServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);
        this.clientSocket = serverSocket.accept();
        System.out.println("Accepted connection from " + clientSocket.getInetAddress());
        this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        this.closed = false;
    }

    @Override
    public void send(String message) throws ChannelException {
        if (closed) {
            throw new ChannelException("Cannot send message; channel is closed.");
        }
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new ChannelException("Error sending message", e);
        }
    }

    @Override
    public String receive() throws ChannelException {
        if (closed) {
            throw new ChannelException("Channel is closed.");
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new ChannelException("End of stream reached");
            }
            return line;
        } catch (IOException e) {
            throw new ChannelException("Error receiving message", e);
        }
    }

    @Override
    public void close() throws ChannelException {
        closed = true;
        try {
            if (clientSocket != null) clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            throw new ChannelException("Error closing socket", e);
        }
    }
}