package orderbook;

/**
 * What happened to the unfilled part of a submitted order (see ADR-0025).
 */
public enum OrderStatus {
    /** Nothing remains; the whole quantity traded. */
    FILLED,
    /** The remainder is now resting in the book (some quantity may also have traded). */
    RESTING,
    /** The remainder was discarded: IOC, market, or a FOK that could not fully fill. */
    CANCELLED,
    /** The order was invalid and never touched the book. */
    REJECTED
}
