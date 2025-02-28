package Producer.BinanceUs;

public sealed interface MarketDataStreamType permits
        MarketDataStreamType.Custom,
        MarketDataStreamType.TradeData,
        MarketDataStreamType.CandlestickData,
        MarketDataStreamType.AggregateTrade,
        MarketDataStreamType.TickerOrder,
        MarketDataStreamType.PartialOrderBookDepth,
        MarketDataStreamType.OrderBookDepthDiff {

    String getStreamName();

    // A custom stream, defined by a user-provided string.
    public record Custom(String streamName) implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return streamName;
        }
    }

    // Represents a trade data stream (without extra details).
    public record TradeData() implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "trade";
        }
    }

    // Represents an aggregated trade data stream.
    public record AggregateTrade() implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "aggTrade";
        }
    }

    // Represents a candlestick stream that requires a time interval.
    public record CandlestickData(CandlestickChartInterval interval) implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "kline_" + interval;
        }
    }

    // Represents a ticker order stream.
    public record TickerOrder() implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "bookTicker";
        }
    }

    // Represents a partial order book depth stream.
    // It requires two parameters: levels (allowed: 5, 10, or 20) and update speed.
    public record PartialOrderBookDepth(DepthLevel level, UpdateSpeed updateSpeed) implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "depth" + level + "@" + (updateSpeed != null ? updateSpeed : "");
        }
    }

    // Represents an order book depth diff stream.
    // It requires one parameter: update speed.
    public record OrderBookDepthDiff(UpdateSpeed updateSpeed) implements MarketDataStreamType {
        @Override
        public String getStreamName() {
            return "depth@" + (updateSpeed != null ? updateSpeed : "");
        }
    }

    // A nested enum for update speed. Allowed speeds are 100ms or 1000ms.
    enum UpdateSpeed {
        MS_100("100ms"),
        MS_1000("1000ms");

        private final String value;

        UpdateSpeed(String value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return value;
        }
    }

    // A nested enum for candlestick chart intervals.
    enum CandlestickChartInterval {
        ONE_MIN("1m"),
        THREE_MIN("3m"),
        FIVE_MIN("5m"),
        FIFTEEN_MIN("15m"),
        THIRTY_MIN("30m"),
        ONE_HOUR("1h"),
        TWO_HOUR("2h"),
        FOUR_HOUR("4h"),
        SIX_HOUR("6h"),
        EIGHT_HOUR("8h"),
        TWELVE_HOUR("12h"),
        ONE_DAY("1d"),
        THREE_DAY("3d"),
        ONE_WEEK("1w"),
        ONE_MONTH("1m");

        private final String value;

        CandlestickChartInterval(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // A nested enum for partial order book depth levels.
    enum DepthLevel {
        LEVEL_5(5),
        LEVEL_10(10),
        LEVEL_20(20);

        private final int value;

        DepthLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}