package MarketDataType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a trade event in a financial market.
 * <p>
 * A {@code Trade} object encapsulates details about a transaction
 * including price, size, buyer/seller information, and execution time.
 * This record is immutable and implements {@link Serializable} for potential
 * persistence or network transmission.
 * </p>
 *
 * @param platform           The name of the exchange/platform where the trade occurred (e.g., "coinbase", "binance.us").
 * @param eventTime          The timestamp when the trade event was received.
 * @param product            The financial instrument that was traded (e.g., "BTCUSD", "ETHUSD").
 * @param tradeId            The unique identifier for the trade.
 * @param price              The price at which the trade was executed.
 * @param size               The quantity of the asset traded.
 * @param buyerId            The identifier of the buyer in the transaction.
 * @param sellerId           The identifier of the seller in the transaction.
 * @param side               Indicates whether the trade was a "buy" or "sell" from the taker's perspective.
 * @param tradeTime          The actual timestamp when the trade was executed on the exchange.
 * @param buyerIsMarketMaker A boolean indicating if the buyer was the market maker in the trade.
 */

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
