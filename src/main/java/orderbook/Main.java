package orderbook;

public class Main {
    public static void main(String[] args) {

        PriceScale scale = new PriceScale(2);
        OrderBook orderBook = new OrderBook(scale);
        MatchingEngine engine = new MatchingEngine(orderBook);

        engine.processOrder(new LimitOrder(1, Side.SELL, 20, scale.toTicks("100.00")));
        engine.processOrder(new LimitOrder(2, Side.SELL, 30, scale.toTicks("100.00")));
        engine.processOrder(new LimitOrder(3, Side.SELL, 40, scale.toTicks("101.00")));

        ExecutionResult result =
                engine.processOrder(new LimitOrder(4, Side.BUY, 60, scale.toTicks("101.00")));

        System.out.println("Order 4: " + result.status() + ", filled " + result.filledQuantity());

        orderBook.printOrderBook();
        orderBook.printTrades();
    }
}
