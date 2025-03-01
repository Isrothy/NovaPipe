package Normalizer.PayloadParser;

import MarketDataType.Trade;
import com.fasterxml.jackson.databind.JsonNode;

import MarketDataType.Ticker;

public interface Parser {
    Ticker parseTicker(JsonNode root);
    Trade parseTrade(JsonNode root);
}
