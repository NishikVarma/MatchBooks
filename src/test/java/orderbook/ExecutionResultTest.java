package orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResultTest {

    private OrderBook orderBook;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
        engine = new MatchingEngine(orderBook);
    }

    // ---- outcomes -------------------------------------------------------

    @Test
    void restingOrderWithNoTradesIsResting() {
        ExecutionResult result = engine.processOrder(new LimitOrder(1, Side.BUY, 10, 100.0));

        assertEquals(OrderStatus.RESTING, result.status());
        assertEquals(0, result.filledQuantity());
        assertEquals(10, result.remainingQuantity());
        assertTrue(result.trades().isEmpty());
        assertNull(result.rejectReason());
    }

    @Test
    void fullFillIsFilled() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 10, 100.0));

        ExecutionResult result = engine.processOrder(new LimitOrder(2, Side.BUY, 10, 100.0));

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(10, result.filledQuantity());
        assertEquals(0, result.remainingQuantity());
        assertEquals(1, result.trades().size());
    }

    @Test
    void partialFillOfGtcOrderRestsTheRemainder() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 4, 100.0));

        ExecutionResult result = engine.processOrder(new LimitOrder(2, Side.BUY, 10, 100.0));

        assertEquals(OrderStatus.RESTING, result.status());
        assertEquals(4, result.filledQuantity());
        assertEquals(6, result.remainingQuantity());
        assertEquals(6, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void partialFillOfIocOrderIsCancelledAndReportsTheDiscardedQuantity() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 4, 100.0));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(2, Side.BUY, 10, 100.0, TimeInForce.IOC));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(4, result.filledQuantity());
        assertEquals(6, result.remainingQuantity());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void marketOrderWithNoLiquidityIsCancelled() {
        ExecutionResult result = engine.processOrder(new MarketOrder(1, Side.BUY, 10));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(0, result.filledQuantity());
        assertEquals(10, result.remainingQuantity());
    }

    @Test
    void killedFokOrderIsCancelledWithNothingFilled() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 4, 100.0));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(2, Side.BUY, 10, 100.0, TimeInForce.FOK));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(0, result.filledQuantity());
        assertEquals(10, result.remainingQuantity());
        assertTrue(orderBook.getTrades().isEmpty());
        assertEquals(4, orderBook.getBestAskOrder().getQuantity());
    }

    @Test
    void fokOrderThatCanFillIsFilled() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 4, 100.0));
        engine.processOrder(new LimitOrder(2, Side.SELL, 6, 101.0));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(3, Side.BUY, 10, 101.0, TimeInForce.FOK));

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(2, result.trades().size());
    }

    @Test
    void resultContainsOnlyThisOrdersTradesInExecutionOrder() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100.0));
        engine.processOrder(new LimitOrder(2, Side.BUY, 5, 100.0));      // earlier trade
        engine.processOrder(new LimitOrder(3, Side.SELL, 5, 100.0));
        engine.processOrder(new LimitOrder(4, Side.SELL, 5, 101.0));

        ExecutionResult result = engine.processOrder(new MarketOrder(5, Side.BUY, 8));

        assertEquals(2, result.trades().size());
        assertEquals(100.0, result.trades().get(0).getPrice());
        assertEquals(5, result.trades().get(0).getQuantity());
        assertEquals(101.0, result.trades().get(1).getPrice());
        assertEquals(3, result.trades().get(1).getQuantity());
        assertEquals(3, orderBook.getTrades().size());                   // history has all three
    }

    @Test
    void resultTradeListCannotBeModified() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100.0));
        ExecutionResult result = engine.processOrder(new MarketOrder(2, Side.BUY, 5));

        assertThrows(UnsupportedOperationException.class, () -> result.trades().clear());
    }

    // ---- validation -----------------------------------------------------

    @Test
    void zeroQuantityIsRejected() {
        assertRejected(RejectReason.INVALID_QUANTITY,
                engine.processOrder(new LimitOrder(1, Side.BUY, 0, 100.0)));
    }

    @Test
    void negativeQuantityIsRejected() {
        assertRejected(RejectReason.INVALID_QUANTITY,
                engine.processOrder(new MarketOrder(1, Side.BUY, -5)));
    }

    @Test
    void nonPositiveOrNonFinitePriceIsRejected() {
        assertRejected(RejectReason.INVALID_PRICE,
                engine.processOrder(new LimitOrder(1, Side.BUY, 10, 0.0)));
        assertRejected(RejectReason.INVALID_PRICE,
                engine.processOrder(new LimitOrder(2, Side.BUY, 10, -1.0)));
        assertRejected(RejectReason.INVALID_PRICE,
                engine.processOrder(new LimitOrder(3, Side.BUY, 10, Double.NaN)));
        assertRejected(RejectReason.INVALID_PRICE,
                engine.processOrder(new LimitOrder(4, Side.SELL, 10, Double.POSITIVE_INFINITY)));
    }

    @Test
    void missingTimeInForceIsRejected() {
        assertRejected(RejectReason.MISSING_TIME_IN_FORCE,
                engine.processOrder(new LimitOrder(1, Side.BUY, 10, 100.0, null)));
        assertRejected(RejectReason.MISSING_TIME_IN_FORCE,
                engine.processOrder(new MarketOrder(2, Side.BUY, 10, null)));
    }

    @Test
    void missingSideIsRejected() {
        assertRejected(RejectReason.INVALID_SIDE,
                engine.processOrder(new LimitOrder(1, null, 10, 100.0)));
    }

    @Test
    void rejectedOrderEchoesSubmittedQuantityAndLeavesTheBookUntouched() {
        ExecutionResult result = engine.processOrder(new LimitOrder(1, Side.BUY, 10, -1.0));

        assertEquals(0, result.filledQuantity());
        assertEquals(10, result.remainingQuantity());
        assertTrue(result.isRejected());
        assertNull(orderBook.getBestBidOrder());
        assertTrue(orderBook.getTrades().isEmpty());
    }

    @Test
    void duplicateIdOfRestingOrderIsRejectedAndFirstOrderStaysCancellable() {
        LimitOrder first = new LimitOrder(7, Side.BUY, 10, 100.0);
        engine.processOrder(first);

        ExecutionResult result = engine.processOrder(new LimitOrder(7, Side.BUY, 5, 101.0));

        assertRejected(RejectReason.DUPLICATE_ID, result);
        assertEquals(100.0, orderBook.getBestBid());          // second order never entered
        orderBook.cancelOrder(7);
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void duplicateIdIsAlsoRejectedForOrdersThatWouldNeverRest() {
        engine.processOrder(new LimitOrder(7, Side.SELL, 10, 100.0));

        assertRejected(RejectReason.DUPLICATE_ID,
                engine.processOrder(new MarketOrder(7, Side.BUY, 10)));
        assertTrue(orderBook.getTrades().isEmpty());
    }

    @Test
    void idOfAFilledOrderMayBeReused() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100.0));
        engine.processOrder(new MarketOrder(2, Side.BUY, 5));      // order 1 is now gone

        ExecutionResult result = engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100.0));

        assertEquals(OrderStatus.RESTING, result.status());
    }

    @Test
    void nullOrderIsAProgrammingErrorAndThrows() {
        assertThrows(NullPointerException.class, () -> engine.processOrder(null));
    }

    private static void assertRejected(RejectReason expected, ExecutionResult result) {
        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(expected, result.rejectReason());
        assertTrue(result.trades().isEmpty());
    }
}
