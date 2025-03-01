package DataChannel.NetworkChannel;

import DataChannel.DataChannel;
import DataChannel.ChannelException;

import java.io.*;
import java.net.Socket;

public class NetworkChannelClient implements DataChannel {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private volatile boolean closed;

    public NetworkChannelClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        System.out.println("Connected to server " + host + ":" + port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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
            socket.close();
        } catch (IOException e) {
            throw new ChannelException("Error closing socket", e);
        }
    }
}