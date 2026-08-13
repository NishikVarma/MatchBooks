package orderbook;

public abstract class Order{
    private final long id;
    private final Side side;
    private int quantity;
    private final OrderType orderType;

    public Order(long id, Side side, int quantity, OrderType orderType){
        this.id = id;
        this.side = side;
        this.quantity = quantity;
        this.orderType = orderType;
    }

    public long getId() {
        return id;
    }

    public Side getSide(){
        return side;
    }

    public int getQuantity(){
        return quantity;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}