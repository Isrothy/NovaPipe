package DataChannel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import DataChannel.ChannelException;
import DataChannel.PipelineChannel;

public class PipelineChannelTest {

    @Test
    public void testSingleMessageThroughPipeline() throws Exception {
        // Create three basic pipes.
        DataChannel pipe1 = new BlockingQueueChannel();
        DataChannel pipe2 = new BlockingQueueChannel();
        DataChannel pipe3 = new BlockingQueueChannel();

        // Chain them together.
        PipelineChannel pipeline = new PipelineChannel(pipe1, pipe2, pipe3);

        String testMessage = "Hello, Pipeline!";
        pipeline.send(testMessage);

        // Allow some time for the forwarding threads to propagate the message.
        Thread.sleep(200);

        // Receive from the output of the pipeline.
        String received = pipeline.receive();
        assertEquals(testMessage, received, "The message received should match the message sent.");

        pipeline.close();
    }

    @Test
    public void testMultipleMessagesThroughPipeline() throws Exception {
        DataChannel pipe1 = new BlockingQueueChannel();
        DataChannel pipe2 = new BlockingQueueChannel();
        DataChannel pipe3 = new BlockingQueueChannel();

        PipelineChannel pipeline = new PipelineChannel(pipe1, pipe2, pipe3);

        int messageCount = 10;
        for (int i = 0; i < messageCount; i++) {
            pipeline.send("Message " + i);
        }

        // Allow time for all messages to be forwarded.
        Thread.sleep(500);

        for (int i = 0; i < messageCount; i++) {
            String received = pipeline.receive();
            assertEquals("Message " + i, received, "Message " + i + " should be forwarded correctly.");
        }

        pipeline.close();
    }

    @Test
    public void testPipelineClose() throws Exception {
        DataChannel pipe1 = new BlockingQueueChannel();
        DataChannel pipe2 = new BlockingQueueChannel();
        DataChannel pipe3 = new BlockingQueueChannel();

        PipelineChannel pipeline = new PipelineChannel(pipe1, pipe2, pipe3);
        pipeline.close();

        // After closing, sending a message should throw a ChannelException.
        assertThrows(ChannelException.class, () -> pipeline.send("Test after close"),
                "Sending after closing should throw a ChannelException.");
    }
}