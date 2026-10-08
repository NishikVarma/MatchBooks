package orderbook;

/**
 * Why an order was rejected (see ADR-0026).
 */
public enum RejectReason {
    INVALID_SIDE,
    MISSING_TIME_IN_FORCE,
    INVALID_TIME_IN_FORCE,
    INVALID_QUANTITY,
    INVALID_PRICE,
    DUPLICATE_ID,
    UNKNOWN_ORDER
}
