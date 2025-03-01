package Utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

/**
 * Utility class for handling JSON operations using Jackson's {@link JsonNode}.
 */
public class JsonUtil {

    /**
     * Extracts a value from a given {@link JsonNode} using the specified extractor function.
     * If the node is {@code null} or contains a JSON null value, this method returns {@code null}.
     *
     * @param node      the {@link JsonNode} from which the value should be extracted
     * @param extractor a {@link Function} defining how to extract the desired value from the node
     * @param <T>       the type of the extracted value
     * @return the extracted value of type {@code T}, or {@code null} if the node is {@code null} or represents a JSON null
     */
    public static <T> T getValue(JsonNode node, Function<JsonNode, T> extractor) {
        return (node != null && !node.isNull()) ? extractor.apply(node) : null;
    }
}
