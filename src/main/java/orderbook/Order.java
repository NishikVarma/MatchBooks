package orderbook;

public class Order{
    private final long id;
    private Side side;
    private int quantity;
    private double price;
    private OrderType orderType;

    public Order(long id, Side side, int quantity, double price, OrderType orderType){
        this.id = id;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
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

    public double getPrice() {
        return price;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}