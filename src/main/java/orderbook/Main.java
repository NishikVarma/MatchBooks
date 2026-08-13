package orderbook;

public class Main {
    public static void main(String[] args) {

        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, 100));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, 100));
        engine.processOrder(new LimitOrder(3, Side.SELL, 40, 101));

        engine.processOrder(new LimitOrder(4, Side.BUY, 60, 101));

        orderBook.printOrderBook();
        orderBook.printTrades();
    }
}