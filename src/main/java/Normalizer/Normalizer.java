package Normalizer;

import DataChannel.ChannelException;
import DataChannel.DataChannel;
import MarketDataType.MarketDataQueryType;
import Normalizer.PayloadParser.BinanceUsPayloadParser;
import Normalizer.PayloadParser.CoinbasePayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code Normalizer} class processes market data messages received from a {@link DataChannel},
 * normalizes them based on the exchange format, and writes the processed data to an output file.
 * <p>
 * This class supports parsing and normalizing data from different cryptocurrency exchanges such as Binance.US and Coinbase.
 * It continuously listens to the channel and processes incoming messages until a poison pill signal is received.
 * </p>
 */
public class Normalizer implements Runnable {

    private final DataChannel channel;
    private final BufferedWriter writer;
    private final ObjectMapper objectMapper;
    private volatile boolean running = true;

    /**
     * Special message that signals the normalizer to stop processing.
     */
    public static final String POISON_PILL = "POISON_PILL";
    private static final Logger logger = LogManager.getLogger(Normalizer.class);

    /**
     * Constructs a {@code Normalizer} that reads data from a {@link DataChannel} and writes
     * the normalized output to a specified file.
     *
     * @param channel        the input channel to receive raw market data.
     * @param outputFilePath the file path where normalized data will be written.
     * @throws IOException if there is an error creating or opening the file.
     */
    public Normalizer(DataChannel channel, String outputFilePath) throws IOException {
        this.channel = channel;
        this.writer = Files.newBufferedWriter(
                Path.of(outputFilePath),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }


    public void stop() {
        running = false;
    }

    /**
     * Continuously listens for incoming messages from the data channel, processes them,
     * and writes normalized data to the output file.
     * <p>
     * If a poison pill message is received, the normalizer stops processing.
     * </p>
     */
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String rawData = channel.receive();
                if (POISON_PILL.equals(rawData)) {
                    logger.info("Received poison pill. Normalizer stopping.");
                    break;
                }
                if (rawData == null) {
                    continue;
                }
                process(rawData);
                logger.info("Processed data: {}", rawData);
            } catch (ChannelException | IOException e) {
                logger.info("Error reading from channel: {}", e.getMessage());
                break;
            }
        }
    }

    /**
     * Parses and processes raw JSON data, normalizes it, and writes the output to a file.
     *
     * @param rawData the raw JSON string received from the channel.
     * @throws IOException if an error occurs while writing to the file.
     */
    private void process(String rawData) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(rawData);
        var obj = parse(jsonNode);
        if (obj != null) {
            String output = objectMapper.writeValueAsString(obj);
            writer.write(output);
            writer.newLine();
            writer.flush();
        } else {
            logger.error("Parsed object is null; nothing to write.");
        }
    }

    /**
     * Extracts and parses data based on the exchange type and market data type.
     *
     * @param root the root JSON node containing market data.
     * @return a parsed and normalized market data object, or {@code null} if parsing fails.
     */
    private Object parse(JsonNode root) {
        String tag = root.get("tag").asText();
        JsonNode payloadNode = root.get("payload");
        Pattern pattern = Pattern.compile("([^@]+)@([^@]+)");
        Matcher matcher = pattern.matcher(tag);
        if (!matcher.matches()) {
            logger.error("Invalid tag format: {}", tag);
            return null;
        }
        String typeStr = matcher.group(1);
        String exchange = matcher.group(2);

        try {
            MarketDataQueryType type = MarketDataQueryType.fromString(typeStr);
            return switch (exchange) {
                case "binance.us" -> new BinanceUsPayloadParser().parse(type, payloadNode);
                case "coinbase" -> new CoinbasePayloadParser().parse(type, payloadNode);
                default -> {
                    logger.error("Unsupported exchange: {}", exchange);
                    yield null;
                }
            };
        } catch (IllegalArgumentException e) {
            logger.error("Failed to parse type: {}", typeStr);
            return null;
        }
    }
}
