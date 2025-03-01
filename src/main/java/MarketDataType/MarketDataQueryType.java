package MarketDataType;

public enum MarketDataQueryType {
    TRADE("trade"),
    QUOTE("quote");

    private final String value;

    MarketDataQueryType(String value) {
        this.value = value;
    }

    public static MarketDataQueryType fromString(String s) {
        for (MarketDataQueryType type : values()) {
            if (type.value.equalsIgnoreCase(s)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for value: " + s);
    }
}
