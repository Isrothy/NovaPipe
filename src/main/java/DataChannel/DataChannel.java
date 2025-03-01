package DataChannel;

public interface DataChannel extends AutoCloseable {

    void send(String message) throws ChannelException;

    String receive() throws ChannelException;

    void close() throws ChannelException;

}
