package orderbook;

public enum TimeInForce {
    /** Good Till Cancelled: the unfilled remainder of a limit order rests in the book. */
    GTC,
    /** Immediate Or Cancel: trade what is possible now, discard the rest. */
    IOC,
    /** Fill Or Kill: trade the whole quantity immediately or do nothing. */
    FOK;

    public boolean restsRemainder() {
        return this == GTC;
    }

    public boolean requiresFullFill() {
        return this == FOK;
    }
}
