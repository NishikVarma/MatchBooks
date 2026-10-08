package orderbook;

public class MarketOrder extends Order {

    /** Convenience constructor: a market order can never rest, so it defaults to IOC, not GTC. */
    public MarketOrder(long id, Side side, int quantity) {
        this(id, side, quantity, TimeInForce.IOC);
    }

    public MarketOrder(long id, Side side, int quantity, TimeInForce timeInForce) {
        super(id, side, quantity, OrderType.MARKET, timeInForce);
    }
}