package Producer.QueryGenerator;

import MarketDataQueryType.MarketDataQueryType;

public class CoinbaseGenerator implements QueryGenerator {

    @Override
    public String generateQueryMessage(String productType, MarketDataQueryType queryType) {
        String channel = switch (queryType) {
            case TRADE -> "matches";
            case QUOTE -> "ticker";
        };
        return String.format("""
            { 
                "type": "subscribe",
                "product_ids": ["%s"],
                "channels": ["%s"]
            }
            """, productType, channel);
    }

    @Override
    public String getUrl() {
        return "wss://ws-feed.exchange.coinbase.com";
    }

    @Override
    public String getTag() {
        return"coinbase";
    }

}
