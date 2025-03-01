package MarketDataType;

/**
 * Enum representing different types of market data queries.
 * <p>
 * This enum is used to specify whether a market data request is for:
 * <ul>
 *     <li>{@link #TRADE} - Trade data (e.g., executed transactions).</li>
 *     <li>{@link #QUOTE} - Quote data (e.g., best bid/ask prices).</li>
 * </ul>
 * Each type is associated with a string value that matches its corresponding WebSocket API request.
 */
public enum MarketDataQueryType {
    TRADE("trade"),
    QUOTE("quote");

    private final String value;

    MarketDataQueryType(String value) {
        this.value = value;
    }

    /**
     * Converts a string to its corresponding {@code MarketDataQueryType} enum value.
     * <p>
     * This method performs a case-insensitive match against the predefined values.
     *
     * @param s the string representation of the market data query type.
     * @return the corresponding {@code MarketDataQueryType} if a match is found.
     * @throws IllegalArgumentException if the provided string does not match any valid type.
     */
    public static MarketDataQueryType fromString(String s) {
        for (MarketDataQueryType type : values()) {
            if (type.value.equalsIgnoreCase(s)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for value: " + s);
    }
}
