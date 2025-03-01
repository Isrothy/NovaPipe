package Normalizer;

import DataChannel.ChannelException;
import DataChannel.DataChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedWriter;

import MarketDataType.MarketDataQueryType;
import Normalizer.PayloadParser.BinanceUsPayloadParser;
import Normalizer.PayloadParser.CoinbasePayloadParser;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Normalizer implements Runnable {

    private final DataChannel channel;
    private final BufferedWriter writer;
    private final ObjectMapper objectMapper;
    private volatile boolean running = true;
    public static final String POISON_PILL = "POISON_PILL";

    public void stop() {
        running = false;
    }

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

    @Override
    public void run() {
        while (running) {
            try {
                String rawData = channel.receive();
                if (POISON_PILL.equals(rawData)) {
                    System.out.println("Received poison pill. Normalizer stopping.");
                    break;
                }
                if (rawData == null) {
                    continue;
                }
                process(rawData);
                System.out.println("Processed data: " + rawData);
            } catch (ChannelException | IOException e) {
                System.err.println("Error reading from channel: " + e.getMessage());
                break;
            }
        }
    }

    private void process(String rawData) throws IOException {
//        System.out.println("Processing data: " + rawData);
        JsonNode jsonNode = objectMapper.readTree(rawData);
        var obj = parse(jsonNode);
        if (obj != null) {
            String output = objectMapper.writeValueAsString(obj);
            writer.write(output);
            writer.newLine();
            writer.flush();
        } else {
            System.err.println("Parsed object is null; nothing to write.");
        }
    }

    private Object parse(JsonNode root) {
        String tag = root.get("tag").asText();
        JsonNode payloadNode = root.get("payload");
        Pattern pattern = Pattern.compile("([^@]+)@([^@]+)");
        Matcher matcher = pattern.matcher(tag);
        if (!matcher.matches()) {
            System.err.printf("Invalid tag format: %s\n", tag);
            return null;
        }
        String typeStr = matcher.group(1);
        String exchange = matcher.group(2);

//        System.out.println("Type: " + typeStr);
//        System.out.println("Exchange: " + exchange);

        try {
            MarketDataQueryType type = MarketDataQueryType.fromString(typeStr);
            return switch (exchange) {
                case "binance.us" -> new BinanceUsPayloadParser().parse(type, payloadNode);
                case "coinbase" -> new CoinbasePayloadParser().parse(type, payloadNode);
                default -> {
                    System.err.printf("Unsupported exchange: %s\n", exchange);
                    yield null;
                }
            };
        } catch (IllegalArgumentException e) {
            System.err.printf("Failed to parse type: %s\n", typeStr);
            return null;
        }


    }
}
