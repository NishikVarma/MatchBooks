package orderbook;

import java.util.List;

/**
 * Outcome of submitting one order (see ADR-0025).
 *
 * @param status            what happened to the unfilled part
 * @param filledQuantity    quantity traded by this order
 * @param remainingQuantity quantity not traded (resting, discarded, or the submitted
 *                          quantity if the order was rejected)
 * @param trades            trades produced by this order, in execution order
 * @param rejectReason      non-null only when status is REJECTED
 */
public record ExecutionResult(
        OrderStatus status,
        int filledQuantity,
        int remainingQuantity,
        List<Trade> trades,
        RejectReason rejectReason) {

    public ExecutionResult {
        trades = List.copyOf(trades);
    }

    static ExecutionResult rejected(int submittedQuantity, RejectReason reason) {
        return new ExecutionResult(OrderStatus.REJECTED, 0, submittedQuantity, List.of(), reason);
    }

    public boolean isRejected() {
        return status == OrderStatus.REJECTED;
    }
}
