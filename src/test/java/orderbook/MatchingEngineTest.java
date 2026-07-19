package orderbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTest {

    @Test
    void testAddSingleBuyOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.BUY, 100, 10.0, OrderType.LIMIT));

        assertEquals(10.0, orderBook.getBestBid());
        assertEquals(0.0, orderBook.getBestAsk());
        assertNotNull(orderBook.getBestBidOrder());
        assertEquals(100, orderBook.getBestBidOrder().getQuantity());
    }

    @Test
    void testFullMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.SELL, 100, 10.0, OrderType.LIMIT));
        engine.processOrder(new Order(2, Side.BUY, 100, 10.0, OrderType.LIMIT));

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

        engine.processOrder(new Order(1, Side.SELL, 200, 10.0, OrderType.LIMIT));
        engine.processOrder(new Order(2, Side.BUY, 75, 10.0, OrderType.LIMIT));

        assertNotNull(orderBook.getBestAskOrder());
        assertEquals(125, orderBook.getBestAskOrder().getQuantity());

        assertEquals(1, orderBook.getTrades().size());
        assertEquals(75, orderBook.getTrades().get(0).getQuantity());
    }

    @Test
    void testNoMatch() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.BUY, 100, 9.5, OrderType.LIMIT));
        engine.processOrder(new Order(2, Side.SELL, 100, 10.5, OrderType.LIMIT));

        assertEquals(9.5, orderBook.getBestBid());
        assertEquals(10.5, orderBook.getBestAsk());

        assertTrue(orderBook.getTrades().isEmpty());
    }

    @Test
    void testFIFO() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.SELL, 100, 10.0, OrderType.LIMIT));
        engine.processOrder(new Order(2, Side.SELL, 100, 10.0, OrderType.LIMIT));

        engine.processOrder(new Order(3, Side.BUY, 150, 10.0, OrderType.LIMIT));

        assertEquals(50, orderBook.getBestAskOrder().getQuantity());

        assertEquals(2, orderBook.getTrades().size());

        assertEquals(100, orderBook.getTrades().get(0).getQuantity());
        assertEquals(50, orderBook.getTrades().get(1).getQuantity());
    }

    @Test
    void testCancelOrder() {
        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.BUY, 100, 10.0, OrderType.LIMIT));

        orderBook.cancelOrder(1);

        assertEquals(0.0, orderBook.getBestBid());
        assertNull(orderBook.getBestBidOrder());
    }

    @Test
    void testOrderTypeIsStored() {
        Order order = new Order(1, Side.BUY, 100, 10.0, OrderType.LIMIT);

        assertEquals(OrderType.LIMIT, order.getOrderType());
    }
}