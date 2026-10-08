package orderbook;

/** Read-only summary of one price level (ADR-0033). */
public record LevelView(long price, long quantity, int orderCount) {
}
