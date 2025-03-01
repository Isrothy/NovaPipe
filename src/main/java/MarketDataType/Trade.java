package MarketDataType;

import java.io.Serializable;

import java.math.BigDecimal;
import java.time.Instant;

public record Trade(
        String platform,
        Instant eventTime,
        String product,
        Long tradeId,
        BigDecimal price,
        BigDecimal size,
        String buyerId,
        String sellerId,
        String side,
        Instant tradeTime,
        Boolean buyerIsMarketMaker
) implements Serializable {
}
