package Normalizer.PayloadParser;

import MarketDataType.MarketDataQueryType;
import MarketDataType.Quote;
import MarketDataType.Trade;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The {@code Parser} interface defines methods for parsing market data messages into structured objects.
 * <p>
 * Implementing classes are responsible for converting raw JSON market data into either a {@link Quote} or a {@link Trade}.
 * </p>
 */
public interface Parser {

    /**
     * Parses a JSON node into a {@link Quote} object.
     *
     * @param root the root JSON node containing quote (ticker) data.
     * @return a {@code Quote} object representing the parsed ticker data.
     */
    Quote parseQuote(JsonNode root);

    /**
     * Parses a JSON node into a {@link Trade} object.
     *
     * @param root the root JSON node containing trade data.
     * @return a {@code Trade} object representing the parsed trade data.
     */
    Trade parseTrade(JsonNode root);

    /**
     * Parses a JSON node based on the specified {@link MarketDataQueryType}.
     * <p>
     * This method serves as a dispatcher, calling the appropriate parsing method
     * based on whether the data represents a trade or a quote.
     * </p>
     *
     * @param type the market data type, determining whether to parse a trade or a quote.
     * @param root the root JSON node containing market data.
     * @return a parsed {@link Trade} or {@link Quote} object, depending on the data type.
     */
    default Object parse(MarketDataQueryType type, JsonNode root) {
        return switch (type) {
            case TRADE -> parseTrade(root);
            case QUOTE -> parseQuote(root);
        };
    }
}
