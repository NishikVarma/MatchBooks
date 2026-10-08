package orderbook;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * One side of the book (ADR-0028): price levels sorted so that the first level is
 * always the best one (bids high to low, asks low to high; ADR-0002), plus a
 * cached reference to that best level (ADR-0029).
 */
final class BookSide {

    private final Side side;
    private final TreeMap<Long, PriceLevel> levels;
    private PriceLevel best;

    BookSide(Side side) {
        this.side = side;
        this.levels = side == Side.BUY
                ? new TreeMap<>(Comparator.reverseOrder())
                : new TreeMap<>();
    }

    boolean isEmpty() {
        return best == null;
    }

    PriceLevel bestLevel() {
        return best;
    }

    /** Oldest order at the best price, or null if the side is empty. */
    LimitOrder bestOrder() {
        return best == null ? null : best.head();
    }

    Iterable<PriceLevel> levels() {
        return levels.values();
    }

    /** Rests the order at the back of its price level, creating the level if needed. */
    void add(LimitOrder order) {
        long price = order.getPrice();
        PriceLevel level = levels.get(price);

        if (level == null) {
            level = new PriceLevel(price);
            levels.put(price, level);
            if (best == null || isBetter(price, best.price())) {
                best = level;
            }
        }

        level.append(order);
    }

    /** Takes a resting order out of the book, dropping its level if it becomes empty. */
    void remove(LimitOrder order) {
        PriceLevel level = order.level;
        level.unlink(order);

        if (level.isEmpty()) {
            levels.remove(level.price());
            if (level == best) {
                best = levels.isEmpty() ? null : levels.firstEntry().getValue();
            }
        }
    }

    /**
     * Can the incoming order trade against the best level of this side?
     * Market orders can whenever the side is not empty. This is the only place
     * where buying and selling differ.
     */
    boolean crossedBy(Order incoming) {
        return best != null && acceptable(best.price(), incoming);
    }

    /** Is there enough quantity at acceptable prices to fill the whole order? */
    boolean canFill(Order incoming) {
        long needed = incoming.getQuantity();
        long available = 0;

        for (PriceLevel level : levels.values()) {
            if (!acceptable(level.price(), incoming)) break;

            available += level.totalQuantity();
            if (available >= needed) return true;
        }

        return false;
    }

    private boolean acceptable(long levelPrice, Order incoming) {
        if (incoming.isMarketOrder()) return true;

        long limit = ((LimitOrder) incoming).getPrice();
        // this side is the one being hit: asks are hit by buyers, bids by sellers
        return side == Side.SELL ? limit >= levelPrice : limit <= levelPrice;
    }

    private boolean isBetter(long candidate, long current) {
        return side == Side.BUY ? candidate > current : candidate < current;
    }
}
