package DataChannel;

public interface DataChannel {

    void send(String message) throws ChannelException;

    String receive() throws ChannelException;

    void close() throws ChannelException;

}
