package Utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

public class JsonUtil {
    public static <T> T getValue(JsonNode node, Function<JsonNode, T> extractor) {
        return (node != null && !node.isNull()) ? extractor.apply(node) : null;
    }
}
