package orderbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTest {

    @Test
    void testAddSingleBuyOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 10));

        assertEquals(10, orderBook.bestBid());
        assertFalse(orderBook.hasAsk());
        assertNotNull(orderBook.getBestBidOrder());
        assertEquals(100, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void testFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 100, 10));
        engine.processOrder(new LimitOrder(2, Side.BUY, 100, 10));

        assertFalse(orderBook.hasBid());
        assertFalse(orderBook.hasAsk());

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(100, trade.getQuantity());
        assertEquals(10, trade.getPrice());
    }

    @Test
    void testPartialFill() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 200, 10));
        engine.processOrder(new LimitOrder(2, Side.BUY, 75, 10));

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(125, orderBook.getBestAskOrder().getQuantity());

        assertEquals(1, orderBook.getTrades().size());
        assertEquals(75, orderBook.getTrades().get(0).getQuantity());
    }

    @Test
    void testNoMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 95));
        engine.processOrder(new LimitOrder(2, Side.SELL, 100, 105));

        assertEquals(95, orderBook.bestBid());
        assertEquals(105, orderBook.bestAsk());

        assertTrue(orderBook.getTrades().isEmpty());
    }

    @Test
    void testFIFO() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 100, 10));
        engine.processOrder(new LimitOrder(2, Side.SELL, 100, 10));

        engine.processOrder(new LimitOrder(3, Side.BUY, 150, 10));

        assertEquals(50, orderBook.getBestAskOrder().getQuantity());

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(100, orderBook.getTrades().get(0).getQuantity());
        assertEquals(50, orderBook.getTrades().get(1).getQuantity());
    }

    @Test
    void testCancelOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 10));

        orderBook.cancelOrder(1);

        assertFalse(orderBook.hasBid());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void testOrderTypeIsStored() {
        Order order = new LimitOrder(1, Side.BUY, 100, 10);

        assertEquals(OrderType.LIMIT, order.getOrderType());
    }

    @Test
    void testMarketBuyFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new MarketOrder(2, Side.BUY, 20));

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(20, trade.getQuantity());
        assertEquals(100, trade.getPrice());

        assertNull(orderBook.getBestAskOrder());
        assertFalse(orderBook.hasAsk());
    }

    @Test
    void testMarketBuyPartialMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 101));

        engine.processOrder(new MarketOrder(3, Side.BUY, 40));

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100, orderBook.getTrades().get(0).getPrice());

        assertEquals(20, orderBook.getTrades().get(1).getQuantity());
        assertEquals(101, orderBook.getTrades().get(1).getPrice());

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(10, orderBook.getBestAskOrder().getQuantity());
        assertEquals(101, orderBook.bestAsk());
    }

    @Test
    void testMarketSellFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100));
        engine.processOrder(new MarketOrder(2, Side.SELL, 20));

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(20, trade.getQuantity());
        assertEquals(100, trade.getPrice());

        assertNull(orderBook.getBestBidOrder());
        assertFalse(orderBook.hasBid());
    }

    @Test
    void testMarketSellPartialMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.BUY, 30, 99));

        engine.processOrder(new MarketOrder(3, Side.SELL, 40));

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100, orderBook.getTrades().get(0).getPrice());

        assertEquals(20, orderBook.getTrades().get(1).getQuantity());
        assertEquals(99, orderBook.getTrades().get(1).getPrice());

        assertNotNull(orderBook.getBestBidOrder());
        assertEquals(10, orderBook.getBestBidOrder().getQuantity());
        assertEquals(99, orderBook.bestBid());
    }

    @Test
    void testMarketOrderWithNoLiquidityIsNotStored() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new MarketOrder(1, Side.BUY, 100));

        assertTrue(orderBook.getTrades().isEmpty());

        assertFalse(orderBook.hasBid());
        assertFalse(orderBook.hasAsk());
        assertNull(orderBook.getBestBidOrder());
        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void testMarketOrderConsumesMultiplePriceLevels() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 101));
        engine.processOrder(new LimitOrder(3, Side.SELL, 40, 102));

        engine.processOrder(new MarketOrder(4, Side.BUY, 60));

        assertEquals(3, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100, orderBook.getTrades().get(0).getPrice());

        assertEquals(30, orderBook.getTrades().get(1).getQuantity());
        assertEquals(101, orderBook.getTrades().get(1).getPrice());

        assertEquals(10, orderBook.getTrades().get(2).getQuantity());
        assertEquals(102, orderBook.getTrades().get(2).getPrice());

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(30, orderBook.getBestAskOrder().getQuantity());
        assertEquals(102, orderBook.bestAsk());
    }

    @Test
    void testIOCPartialFill() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.BUY, 50, 100, TimeInForce.IOC));

        assertEquals(1, orderBook.getTrades().size());
        assertEquals(20, orderBook.getTrades().get(0).getQuantity());

        assertFalse(orderBook.hasBid());
        assertNull(orderBook.getBestBidOrder());
        assertFalse(orderBook.hasAsk());
    }

    @Test
    void testFOKFullFill() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 101));

        engine.processOrder(
                new LimitOrder(3, Side.BUY, 50, 101, TimeInForce.FOK)
        );

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100, orderBook.getTrades().get(0).getPrice());

        assertEquals(30, orderBook.getTrades().get(1).getQuantity());
        assertEquals(101, orderBook.getTrades().get(1).getPrice());

        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void testFOKCannotFill() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));

        engine.processOrder(
                new LimitOrder(2, Side.BUY, 50, 100, TimeInForce.FOK)
        );

        assertTrue(orderBook.getTrades().isEmpty());

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(20, orderBook.getBestAskOrder().getQuantity());
        assertEquals(100, orderBook.bestAsk());
    }

    @Test
    void testFOKRespectsLimitPrice() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 102));

        engine.processOrder(
                new LimitOrder(3, Side.BUY, 50, 101, TimeInForce.FOK)
        );

        assertTrue(orderBook.getTrades().isEmpty());

        assertEquals(20, orderBook.getBestAskOrder().getQuantity());
        assertEquals(100, orderBook.bestAsk());
    }


}