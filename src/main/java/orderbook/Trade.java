package orderbook;
import java.time.Instant;

public class Trade {
    private final long tradeId;
    private final long price;
    private final int quantity;
    private final long buyOrderId;
    private final long sellOrderId;
    private final Side aggressorSide;
    private final Instant timestamp;

    Trade(long tradeId, long price, int quantity, long buyOrderId, long sellOrderId, Side aggressorSide, Instant timestamp){
        this.tradeId = tradeId;
        this.price = price;
        this.quantity = quantity;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.aggressorSide = aggressorSide;
        this.timestamp = timestamp;
    }

    /** Sequence number within one order book, starting at 1 (ADR-0033). */
    public long getTradeId() {
        return tradeId;
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

    /** Side of the incoming order that caused this trade. */
    public Side getAggressorSide() {
        return aggressorSide;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
