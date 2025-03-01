package Producer.QueryGenerator;

import MarketDataQueryType.MarketDataQueryType;

public interface QueryGenerator {
    String generateQueryMessage(String productType, MarketDataQueryType queryType);

    String getUrl();

    String getTag();
}
