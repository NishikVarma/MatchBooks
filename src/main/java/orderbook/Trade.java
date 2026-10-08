package orderbook;
import java.time.Instant;

public class Trade {
    private final long price;
    private final int quantity;
    private final long buyOrderId;
    private final long sellOrderId;
    private final Instant timestamp;

    Trade(long price, int quantity, long buyOrderId, long sellOrderId, Instant timestamp){
        this.price = price;
        this.quantity = quantity;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.timestamp = timestamp;
    }

    public long getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
