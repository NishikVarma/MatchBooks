package orderbook;

public class LimitOrder extends Order{
    private final long price;

    // Intrusive list links, owned by PriceLevel while the order rests (ADR-0029).
    LimitOrder prev;
    LimitOrder next;
    PriceLevel level;

    /** Convenience constructor: a limit order rests until filled or cancelled (GTC). */
    public LimitOrder(long id, Side side, int quantity, long price) {
        this(id, side, quantity, price, TimeInForce.GTC);
    }

    public LimitOrder(long id, Side side, int quantity, long price, TimeInForce timeInForce) {
        super(id, side, quantity, OrderType.LIMIT, timeInForce);
        this.price = price;
    }

    public long getPrice(){
        return price;
    }
}