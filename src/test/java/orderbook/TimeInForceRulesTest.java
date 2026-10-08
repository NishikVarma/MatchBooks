package orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Order type x time-in-force matrix (ADR-0027). */
class TimeInForceRulesTest {

    private OrderBook orderBook;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
        engine = new MatchingEngine(orderBook);
    }

    // ---- market + FOK -----------------------------------------------------

    @Test
    void marketFokBuyFillsAcrossLevelsWhenEnoughLiquidity() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 150));

        ExecutionResult result =
                engine.processOrder(new MarketOrder(3, Side.BUY, 50, TimeInForce.FOK));

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(2, result.trades().size());
        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void marketFokBuyIsKilledWhenLiquidityIsShort() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));

        ExecutionResult result =
                engine.processOrder(new MarketOrder(2, Side.BUY, 50, TimeInForce.FOK));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(0, result.filledQuantity());
        assertEquals(50, result.remainingQuantity());
        assertTrue(orderBook.getTrades().isEmpty());
        assertEquals(20, orderBook.getBestAskOrder().getQuantity());
    }

    @Test
    void marketFokSellFillsAndIsKilledSymmetrically() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100));

        ExecutionResult killed =
                engine.processOrder(new MarketOrder(2, Side.SELL, 30, TimeInForce.FOK));
        assertEquals(OrderStatus.CANCELLED, killed.status());
        assertEquals(20, orderBook.getBestBidOrder().getQuantity());

        ExecutionResult filled =
                engine.processOrder(new MarketOrder(3, Side.SELL, 20, TimeInForce.FOK));
        assertEquals(OrderStatus.FILLED, filled.status());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void marketFokOnEmptyBookIsKilled() {
        ExecutionResult result =
                engine.processOrder(new MarketOrder(1, Side.BUY, 5, TimeInForce.FOK));

        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    // ---- market + GTC -----------------------------------------------------

    @Test
    void marketGtcIsRejected() {
        ExecutionResult result =
                engine.processOrder(new MarketOrder(1, Side.BUY, 5, TimeInForce.GTC));

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(RejectReason.INVALID_TIME_IN_FORCE, result.rejectReason());
    }

    // ---- sell-side limit IOC / FOK ---------------------------------------

    @Test
    void sellIocFillsWhatCrossesAndDiscardsTheRest() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(2, Side.SELL, 50, 100, TimeInForce.IOC));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(20, result.filledQuantity());
        assertEquals(30, result.remainingQuantity());
        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void sellIocThatDoesNotCrossLeavesNothingBehind() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 99));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(2, Side.SELL, 10, 100, TimeInForce.IOC));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(0, result.filledQuantity());
        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void sellFokRespectsLimitPriceAndQuantity() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.BUY, 30, 98));

        ExecutionResult tooFar =
                engine.processOrder(new LimitOrder(3, Side.SELL, 50, 99, TimeInForce.FOK));
        assertEquals(OrderStatus.CANCELLED, tooFar.status());
        assertTrue(orderBook.getTrades().isEmpty());

        ExecutionResult ok =
                engine.processOrder(new LimitOrder(4, Side.SELL, 50, 98, TimeInForce.FOK));
        assertEquals(OrderStatus.FILLED, ok.status());
        assertEquals(2, ok.trades().size());
    }

    @Test
    void fokCountsQuantityAcrossSeveralOrdersAtOneLevel() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 10, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 10, 100));
        engine.processOrder(new LimitOrder(3, Side.SELL, 10, 100));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(4, Side.BUY, 25, 100, TimeInForce.FOK));

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(5, orderBook.getBestAskOrder().getQuantity());
    }

    // ---- enum behaviour -------------------------------------------------

    @Test
    void enumDescribesItsOwnRules() {
        assertTrue(TimeInForce.GTC.restsRemainder());
        assertFalse(TimeInForce.IOC.restsRemainder());
        assertFalse(TimeInForce.FOK.restsRemainder());
        assertTrue(TimeInForce.FOK.requiresFullFill());
        assertFalse(TimeInForce.GTC.requiresFullFill());
        assertFalse(TimeInForce.IOC.requiresFullFill());
    }
}
