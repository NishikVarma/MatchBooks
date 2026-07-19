package orderbook;

public class Main {
    public static void main(String[] args) {

        OrderBook orderBook = new OrderBook();
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new Order(1, Side.SELL, 20, 100, OrderType.LIMIT));
        engine.processOrder(new Order(2, Side.SELL, 30, 100, OrderType.LIMIT));
        engine.processOrder(new Order(3, Side.SELL, 40, 101, OrderType.LIMIT));

        engine.processOrder(new Order(4, Side.BUY, 60, 101, OrderType.LIMIT));

        orderBook.printOrderBook();
        orderBook.printTrades();
    }
}