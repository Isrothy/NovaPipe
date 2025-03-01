package Producer.QueryGenerator;

import MarketDataType.MarketDataQueryType;

public interface QueryGenerator {
    String generateQueryMessage(String productType, MarketDataQueryType queryType);

    String getUrl();

    String getTag();
}
