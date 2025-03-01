package MarketDataType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a market data quote containing real-time trading information.
 * <p>
 * A {@code Quote} object encapsulates bid/ask prices, trade prices, and
 * other relevant market data for a specific financial instrument.
 * This record is immutable and implements {@link Serializable} for potential
 * persistence or transmission over networks.
 * </p>
 *
 * @param platform     The name of the exchange/platform providing the quote (e.g., "coinbase", "binance.us").
 * @param sequence     The sequence number of the quote update, useful for tracking real-time changes.
 * @param product      The financial instrument being traded (e.g., "BTCUSD", "ETHUSD").
 * @param bestBid      The best bid price available for the product.
 * @param bestBidSize  The quantity available at the best bid price.
 * @param bestAsk      The best ask price available for the product.
 * @param bestAskSize  The quantity available at the best ask price.
 * @param price        The last traded price.
 * @param open24h      The opening price of the product in the last 24 hours.
 * @param volume24h    The total trading volume of the product in the last 24 hours.
 * @param low24h       The lowest price of the product in the last 24 hours.
 * @param high24h      The highest price of the product in the last 24 hours.
 * @param volume30d    The total trading volume of the product in the last 30 days.
 * @param side         The last trade direction (e.g., "buy" or "sell").
 * @param time         The timestamp of when the quote was received.
 * @param trade_id     The identifier of the last executed trade.
 * @param last_size    The size of the last executed trade.
 */
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
