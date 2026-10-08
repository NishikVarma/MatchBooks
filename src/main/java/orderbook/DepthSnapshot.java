package orderbook;

import java.util.List;

/**
 * Immutable copy of the top of both sides of the book, best price first (ADR-0033).
 */
public record DepthSnapshot(List<LevelView> bids, List<LevelView> asks) {

    public DepthSnapshot {
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }
}
