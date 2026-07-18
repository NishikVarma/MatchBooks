public class Order{
    private Side side;
    private int quantity;
    private double price;

    Order(Side side, int quantity, double price){
        this.side = side;
        this.quantity = quantity;
        this.price = price;
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

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}