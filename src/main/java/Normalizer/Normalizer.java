package Normalizer;

import DataChannel.ChannelException;
import DataChannel.DataChannel;

import java.io.BufferedWriter;

import Normalizer.PayloadParser.BinanceUsPayloadParser;
import Normalizer.PayloadParser.CoinbasePayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
                process(rawData);
                System.out.println("Processed data: " + rawData);
            } catch (ChannelException | IOException e) {
                System.err.println("Error reading from channel: " + e.getMessage());
                break;
            }
        }
    }

    private void process(String rawData) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(rawData);
        String data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        writer.write(data);
        writer.newLine();
        writer.flush();
    }

    private Object parse(JsonNode root) {
        String tag = root.get("tag").asText();
        JsonNode payloadNode = root.get("payload");
        Pattern pattern = Pattern.compile("([^@]+)@([^@]+)");
        Matcher matcher = pattern.matcher(tag);
        if (!matcher.matches()) {
            System.err.printf("Invalid tag format: %s", tag);
            return null;
        }
        String symbol = matcher.group(1);
        String exchange = matcher.group(2);

        return switch (exchange) {
            case "binance.us" -> new BinanceUsPayloadParser().parseTicker(payloadNode);
            case "coinbase" -> new CoinbasePayloadParser().parseTicker(payloadNode);
            default -> {
                System.err.printf("Unsupported exchange: %s", exchange);
                yield null;
            }
        };
    }
}
