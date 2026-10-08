package orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Trade identity, aggressor side, clock (ADR-0033, ADR-0034) and depth snapshots. */
class TradeAndDepthTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

    private OrderBook orderBook;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
        engine = new MatchingEngine(orderBook, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void tradeIdsAreASequenceStartingAtOne() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 5, 101));
        engine.processOrder(new MarketOrder(3, Side.BUY, 10));

        List<Trade> trades = orderBook.getTrades();
        assertEquals(1, trades.get(0).getTradeId());
        assertEquals(2, trades.get(1).getTradeId());
    }

    @Test
    void tradeIdsAreIndependentPerBook() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100));
        engine.processOrder(new MarketOrder(2, Side.BUY, 5));

        OrderBook other = new OrderBook();
        MatchingEngine otherEngine = new MatchingEngine(other);
        otherEngine.processOrder(new LimitOrder(1, Side.SELL, 5, 100));
        otherEngine.processOrder(new MarketOrder(2, Side.BUY, 5));

        assertEquals(1, other.getTrades().get(0).getTradeId());
    }

    @Test
    void aggressorIsTheIncomingSide() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100));
        engine.processOrder(new MarketOrder(2, Side.BUY, 5));
        engine.processOrder(new LimitOrder(3, Side.BUY, 5, 100));
        engine.processOrder(new MarketOrder(4, Side.SELL, 5));

        assertEquals(Side.BUY, orderBook.getTrades().get(0).getAggressorSide());
        assertEquals(Side.SELL, orderBook.getTrades().get(1).getAggressorSide());
    }

    @Test
    void buyAndSellOrderIdsAreAssignedByRoleNotByAggression() {
        engine.processOrder(new LimitOrder(10, Side.BUY, 5, 100));
        engine.processOrder(new MarketOrder(20, Side.SELL, 5));

        Trade trade = orderBook.getTrades().get(0);
        assertEquals(10, trade.getBuyOrderId());
        assertEquals(20, trade.getSellOrderId());
    }

    @Test
    void timestampsComeFromTheInjectedClock() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 100));
        engine.processOrder(new MarketOrder(2, Side.BUY, 5));

        assertEquals(NOW, orderBook.getTrades().get(0).getTimestamp());
    }

    // ---- depth ----------------------------------------------------------

    @Test
    void depthAggregatesQuantityAndOrderCountPerLevelBestFirst() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 10, 99));
        engine.processOrder(new LimitOrder(2, Side.BUY, 20, 99));
        engine.processOrder(new LimitOrder(3, Side.BUY, 5, 98));
        engine.processOrder(new LimitOrder(4, Side.SELL, 7, 101));
        engine.processOrder(new LimitOrder(5, Side.SELL, 8, 103));

        DepthSnapshot depth = orderBook.depth(10);

        assertEquals(List.of(new LevelView(99, 30, 2), new LevelView(98, 5, 1)), depth.bids());
        assertEquals(List.of(new LevelView(101, 7, 1), new LevelView(103, 8, 1)), depth.asks());
    }

    @Test
    void depthIsLimitedToTheRequestedNumberOfLevels() {
        for (int i = 0; i < 5; i++) {
            engine.processOrder(new LimitOrder(i + 1, Side.SELL, 1, 100 + i));
        }

        DepthSnapshot depth = orderBook.depth(3);

        assertEquals(3, depth.asks().size());
        assertEquals(100, depth.asks().get(0).price());
        assertTrue(depth.bids().isEmpty());
    }

    @Test
    void depthReflectsPartialFillsAndCancels() {
        engine.processOrder(new LimitOrder(1, Side.SELL, 10, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 10, 100));
        engine.processOrder(new MarketOrder(3, Side.BUY, 4));
        engine.cancelOrder(2);

        assertEquals(List.of(new LevelView(100, 6, 1)), orderBook.depth(5).asks());
    }

    @Test
    void depthSnapshotIsACopy() {
        engine.processOrder(new LimitOrder(1, Side.BUY, 10, 99));
        DepthSnapshot before = orderBook.depth(5);

        engine.processOrder(new MarketOrder(2, Side.SELL, 10));

        assertEquals(1, before.bids().size());                 // snapshot unchanged
        assertTrue(orderBook.depth(5).bids().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> before.bids().clear());
    }
}
