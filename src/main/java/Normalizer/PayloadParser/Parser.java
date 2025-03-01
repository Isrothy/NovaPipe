package Normalizer.PayloadParser;

import MarketDataType.MarketDataQueryType;
import MarketDataType.Trade;
import com.fasterxml.jackson.databind.JsonNode;

import MarketDataType.Quote;

import java.io.Serializable;

public interface Parser {
    Quote parseTicker(JsonNode root);

    Trade parseTrade(JsonNode root);

    default Object parse(MarketDataQueryType type, JsonNode root) {
        return switch (type) {
            case TRADE -> parseTrade(root);
            case QUOTE -> parseTicker(root);
        };
    }
}
