package orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cancel (ADR-0031) and the linked-list level structure it relies on (ADR-0029). */
class CancelReplaceTest {

    private OrderBook orderBook;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
        engine = new MatchingEngine(orderBook);
    }

    private void rest(long id, Side side, int qty, long price) {
        engine.processOrder(new LimitOrder(id, side, qty, price));
    }

    // ---- cancel ---------------------------------------------------------

    @Test
    void cancelReturnsTrueForRestingAndFalseOtherwise() {
        rest(1, Side.BUY, 10, 100);

        assertTrue(orderBook.cancelOrder(1));
        assertFalse(orderBook.cancelOrder(1));      // already gone
        assertFalse(orderBook.cancelOrder(999));    // never existed
    }

    @Test
    void cancelOfAFilledOrderReturnsFalse() {
        rest(1, Side.SELL, 10, 100);
        engine.processOrder(new MarketOrder(2, Side.BUY, 10));

        assertFalse(engine.cancelOrder(1));
    }

    @Test
    void cancelInTheMiddleOfALevelKeepsTheOthersInOrder() {
        rest(1, Side.SELL, 10, 100);
        rest(2, Side.SELL, 20, 100);
        rest(3, Side.SELL, 30, 100);

        assertTrue(engine.cancelOrder(2));

        ExecutionResult result = engine.processOrder(new MarketOrder(4, Side.BUY, 40));
        assertEquals(2, result.trades().size());
        assertEquals(1, result.trades().get(0).getSellOrderId());
        assertEquals(10, result.trades().get(0).getQuantity());
        assertEquals(3, result.trades().get(1).getSellOrderId());
        assertEquals(30, result.trades().get(1).getQuantity());
    }

    @Test
    void cancelOfHeadAndTailLeavesTheMiddle() {
        rest(1, Side.BUY, 10, 100);
        rest(2, Side.BUY, 20, 100);
        rest(3, Side.BUY, 30, 100);

        engine.cancelOrder(1);
        engine.cancelOrder(3);

        assertEquals(2, orderBook.getBestBidOrder().getId());
        assertEquals(20, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void cancellingTheLastOrderAtTheBestPriceMovesBestToTheNextLevel() {
        rest(1, Side.BUY, 10, 100);
        rest(2, Side.BUY, 10, 99);
        rest(3, Side.BUY, 10, 98);

        engine.cancelOrder(1);
        assertEquals(99, orderBook.bestBid());

        engine.cancelOrder(2);
        assertEquals(98, orderBook.bestBid());

        engine.cancelOrder(3);
        assertFalse(orderBook.hasBid());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void cancellingANonBestLevelLeavesTheBestAlone() {
        rest(1, Side.SELL, 10, 100);
        rest(2, Side.SELL, 10, 105);

        engine.cancelOrder(2);

        assertEquals(100, orderBook.bestAsk());
    }

    @Test
    void cancelledQuantityNoLongerCountsTowardsFokLiquidity() {
        rest(1, Side.SELL, 10, 100);
        rest(2, Side.SELL, 10, 100);
        engine.cancelOrder(2);

        ExecutionResult result =
                engine.processOrder(new LimitOrder(3, Side.BUY, 20, 100, TimeInForce.FOK));

        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    @Test
    void partiallyFilledQuantityNoLongerCountsTowardsFokLiquidity() {
        rest(1, Side.SELL, 10, 100);
        engine.processOrder(new MarketOrder(2, Side.BUY, 4));   // 6 left

        assertEquals(OrderStatus.CANCELLED,
                engine.processOrder(new LimitOrder(3, Side.BUY, 7, 100, TimeInForce.FOK)).status());
        assertEquals(OrderStatus.FILLED,
                engine.processOrder(new LimitOrder(4, Side.BUY, 6, 100, TimeInForce.FOK)).status());
    }

    @Test
    void idCanBeReusedAfterCancel() {
        rest(1, Side.BUY, 10, 100);
        engine.cancelOrder(1);

        ExecutionResult result = engine.processOrder(new LimitOrder(1, Side.BUY, 5, 101));

        assertEquals(OrderStatus.RESTING, result.status());
        assertEquals(101, orderBook.bestBid());
    }

    // ---- replace --------------------------------------------------------

    @Test
    void replaceOfUnknownIdIsRejected() {
        ExecutionResult result = engine.replaceOrder(42, 10, 100);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(RejectReason.UNKNOWN_ORDER, result.rejectReason());
    }

    @Test
    void invalidReplaceValuesAreRejectedAndDoNotCancelTheOrder() {
        rest(1, Side.BUY, 10, 100);

        assertEquals(RejectReason.INVALID_QUANTITY, engine.replaceOrder(1, 0, 100).rejectReason());
        assertEquals(RejectReason.INVALID_PRICE, engine.replaceOrder(1, 10, -5).rejectReason());

        assertEquals(10, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void reducingQuantityAtTheSamePriceKeepsTimePriority() {
        rest(1, Side.SELL, 50, 100);
        rest(2, Side.SELL, 50, 100);

        ExecutionResult result = engine.replaceOrder(1, 20, 100);

        assertEquals(OrderStatus.RESTING, result.status());
        assertEquals(20, result.remainingQuantity());
        assertEquals(1, orderBook.getBestAskOrder().getId());      // still first
        assertEquals(20, orderBook.getBestAskOrder().getQuantity());

        ExecutionResult buy = engine.processOrder(new MarketOrder(3, Side.BUY, 30));
        assertEquals(1, buy.trades().get(0).getSellOrderId());
        assertEquals(20, buy.trades().get(0).getQuantity());
        assertEquals(2, buy.trades().get(1).getSellOrderId());
    }

    @Test
    void reducedQuantityIsReflectedInFokLiquidity() {
        rest(1, Side.SELL, 50, 100);
        engine.replaceOrder(1, 20, 100);

        assertEquals(OrderStatus.CANCELLED,
                engine.processOrder(new LimitOrder(2, Side.BUY, 30, 100, TimeInForce.FOK)).status());
    }

    @Test
    void increasingQuantityMovesTheOrderToTheBackOfTheLine() {
        rest(1, Side.SELL, 10, 100);
        rest(2, Side.SELL, 10, 100);

        engine.replaceOrder(1, 15, 100);

        assertEquals(2, orderBook.getBestAskOrder().getId());
        ExecutionResult buy = engine.processOrder(new MarketOrder(3, Side.BUY, 25));
        assertEquals(2, buy.trades().get(0).getSellOrderId());
        assertEquals(1, buy.trades().get(1).getSellOrderId());
        assertEquals(15, buy.trades().get(1).getQuantity());
    }

    @Test
    void changingThePriceMovesTheOrderToTheNewLevel() {
        rest(1, Side.BUY, 10, 100);

        ExecutionResult result = engine.replaceOrder(1, 10, 101);

        assertEquals(OrderStatus.RESTING, result.status());
        assertEquals(101, orderBook.bestBid());
        assertEquals(1, orderBook.getBestBidOrder().getId());
    }

    @Test
    void replacingToACrossingPriceTradesImmediately() {
        rest(1, Side.SELL, 10, 100);
        rest(2, Side.BUY, 10, 95);

        ExecutionResult result = engine.replaceOrder(2, 10, 100);

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(1, result.trades().size());
        assertEquals(100, result.trades().get(0).getPrice());
        assertNull(orderBook.getBestBidOrder());
    }
}
