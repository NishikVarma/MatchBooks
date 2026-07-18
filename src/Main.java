public class Main {
    public static void main(String[] args){
        OrderBook orderBook = new OrderBook();

        orderBook.processOrder(new Order(Side.SELL, 20, 100));
        orderBook.processOrder(new Order(Side.SELL, 30, 100));
        orderBook.processOrder(new Order(Side.SELL, 40, 101));

        orderBook.processOrder(new Order(Side.BUY, 60, 101));

        orderBook.printOrderBook();
    }
}
