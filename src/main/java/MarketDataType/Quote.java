package MarketDataType;

import java.io.Serializable;

import java.math.BigDecimal;
import java.time.Instant;

public record Quote(
        String platform,
        long sequence,
        String product,
        BigDecimal bestBid,
        BigDecimal bestBidSize,
        BigDecimal bestAsk,
        BigDecimal bestAskSize,
        BigDecimal price,
        BigDecimal open24h,
        BigDecimal volume24h,
        BigDecimal low24h,
        BigDecimal high24h,
        BigDecimal volume30d,
        String side,
        Instant time,
        String trade_id,
        BigDecimal last_size
) implements Serializable {

}
