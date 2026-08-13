package orderbook;

public class MarketOrder extends Order {

    public MarketOrder(long id, Side side, int quantity) {
        super(id, side, quantity, OrderType.MARKET);
    }
}