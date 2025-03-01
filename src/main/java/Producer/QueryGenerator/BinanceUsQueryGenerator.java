package Producer.QueryGenerator;

import MarketDataQueryType.MarketDataQueryType;

public class BinanceUsQueryGenerator implements QueryGenerator {
    private static int idCounter = 1;

    @Override
    public String generateQueryMessage(String productType, MarketDataQueryType queryType) {
        String param = productType + "@" + (switch (queryType) {
            case TRADE -> "trade";
            case QUOTE -> "bookTicker";
        });
        String subscribeMessage = String.format("""
            {
                "method": "SUBSCRIBE",
                "params": ["%s"],
                "id": %d
            }
            """, param, idCounter);
        idCounter += 1;
        return subscribeMessage;
    }

    @Override
    public String getUrl() {
        return "wss://stream.binance.us:9443/ws";
    }
}
