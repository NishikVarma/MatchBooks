package orderbook;

import java.math.BigDecimal;

/**
 * Converts between human-readable decimal prices and integer ticks (ADR-0032).
 * The matching core only ever sees ticks; a scale of 2 means one tick is 0.01.
 */
public record PriceScale(int decimals) {

    /** Prices are shown as raw tick counts. */
    public static final PriceScale TICKS = new PriceScale(0);

    public PriceScale {
        if (decimals < 0 || decimals > 18) {
            throw new IllegalArgumentException("decimals must be between 0 and 18: " + decimals);
        }
    }

    /**
     * @throws IllegalArgumentException if the text has more decimals than this scale
     *                                  allows, or does not fit in a long
     */
    public long toTicks(String price) {
        try {
            return new BigDecimal(price).movePointRight(decimals).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("price not representable at scale " + decimals + ": " + price, e);
        }
    }

    public String format(long ticks) {
        return BigDecimal.valueOf(ticks, decimals).toPlainString();
    }
}
