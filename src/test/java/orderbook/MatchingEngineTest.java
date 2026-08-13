package orderbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTest {

    @Test
    void testAddSingleBuyOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 10.0));

        assertEquals(10.0, orderBook.getBestBid());
        assertEquals(0.0, orderBook.getBestAsk());
        assertNotNull(orderBook.getBestBidOrder());
        assertEquals(100, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void testFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 100, 10.0));
        engine.processOrder(new LimitOrder(2, Side.BUY, 100, 10.0));

        assertEquals(0.0, orderBook.getBestBid());
        assertEquals(0.0, orderBook.getBestAsk());

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(100, trade.getQuantity());
        assertEquals(10.0, trade.getPrice());
    }

    @Test
    void testPartialFill() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 200, 10.0));
        engine.processOrder(new LimitOrder(2, Side.BUY, 75, 10.0));

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(125, orderBook.getBestAskOrder().getQuantity());

        assertEquals(1, orderBook.getTrades().size());
        assertEquals(75, orderBook.getTrades().get(0).getQuantity());
    }

    @Test
    void testNoMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 9.5));
        engine.processOrder(new LimitOrder(2, Side.SELL, 100, 10.5));

        assertEquals(9.5, orderBook.getBestBid());
        assertEquals(10.5, orderBook.getBestAsk());

        assertTrue(orderBook.getTrades().isEmpty());
    }

    @Test
    void testFIFO() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 100, 10.0));
        engine.processOrder(new LimitOrder(2, Side.SELL, 100, 10.0));

        engine.processOrder(new LimitOrder(3, Side.BUY, 150, 10.0));

        assertEquals(50, orderBook.getBestAskOrder().getQuantity());

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(100, orderBook.getTrades().get(0).getQuantity());
        assertEquals(50, orderBook.getTrades().get(1).getQuantity());
    }

    @Test
    void testCancelOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 100, 10.0));

        orderBook.cancelOrder(1);

        assertEquals(0.0, orderBook.getBestBid());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void testOrderTypeIsStored() {
        Order order = new LimitOrder(1, Side.BUY, 100, 10.0);

        assertEquals(OrderType.LIMIT, order.getOrderType());
    }

    @Test
    void testMarketBuyFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100.0));
        engine.processOrder(new MarketOrder(2, Side.BUY, 20));

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(20, trade.getQuantity());
        assertEquals(100.0, trade.getPrice());

        assertNull(orderBook.getBestAskOrder());
        assertEquals(0.0, orderBook.getBestAsk());
    }

    @Test
    void testMarketBuyPartialMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100.0));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 101.0));

        engine.processOrder(new MarketOrder(3, Side.BUY, 40));

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100.0, orderBook.getTrades().get(0).getPrice());

        assertEquals(20, orderBook.getTrades().get(1).getQuantity());
        assertEquals(101.0, orderBook.getTrades().get(1).getPrice());

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(10, orderBook.getBestAskOrder().getQuantity());
        assertEquals(101.0, orderBook.getBestAsk());
    }

    @Test
    void testMarketSellFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100.0));
        engine.processOrder(new MarketOrder(2, Side.SELL, 20));

        assertEquals(1, orderBook.getTrades().size());

        Trade trade = orderBook.getTrades().get(0);

        assertEquals(20, trade.getQuantity());
        assertEquals(100.0, trade.getPrice());

        assertNull(orderBook.getBestBidOrder());
        assertEquals(0.0, orderBook.getBestBid());
    }

    @Test
    void testMarketSellPartialMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.BUY, 20, 100.0));
        engine.processOrder(new LimitOrder(2, Side.BUY, 30, 99.0));

        engine.processOrder(new MarketOrder(3, Side.SELL, 40));

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100.0, orderBook.getTrades().get(0).getPrice());

        assertEquals(20, orderBook.getTrades().get(1).getQuantity());
        assertEquals(99.0, orderBook.getTrades().get(1).getPrice());

        assertNotNull(orderBook.getBestBidOrder());
        assertEquals(10, orderBook.getBestBidOrder().getQuantity());
        assertEquals(99.0, orderBook.getBestBid());
    }

    @Test
    void testMarketOrderWithNoLiquidityIsNotStored() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new MarketOrder(1, Side.BUY, 100));

        assertTrue(orderBook.getTrades().isEmpty());

        assertEquals(0.0, orderBook.getBestBid());
        assertEquals(0.0, orderBook.getBestAsk());
        assertNull(orderBook.getBestBidOrder());
        assertNull(orderBook.getBestAskOrder());
    }

    @Test
    void testMarketOrderConsumesMultiplePriceLevels() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100.0));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 101.0));
        engine.processOrder(new LimitOrder(3, Side.SELL, 40, 102.0));

        engine.processOrder(new MarketOrder(4, Side.BUY, 60));

        assertEquals(3, orderBook.getTrades().size());

        assertEquals(20, orderBook.getTrades().get(0).getQuantity());
        assertEquals(100.0, orderBook.getTrades().get(0).getPrice());

        assertEquals(30, orderBook.getTrades().get(1).getQuantity());
        assertEquals(101.0, orderBook.getTrades().get(1).getPrice());

        assertEquals(10, orderBook.getTrades().get(2).getQuantity());
        assertEquals(102.0, orderBook.getTrades().get(2).getPrice());

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(30, orderBook.getBestAskOrder().getQuantity());
        assertEquals(102.0, orderBook.getBestAsk());
    }
}