package orderbook;

public class LimitOrder extends Order{
    private final double price;

    public LimitOrder(long id, Side side, int quantity, double price) {
        super(id, side, quantity, OrderType.LIMIT);
        this.price = price;
    }

    public double getPrice(){
        return price;
    }
}