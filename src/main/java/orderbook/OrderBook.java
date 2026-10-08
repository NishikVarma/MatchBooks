package orderbook;

import java.util.*;

public class OrderBook {
    private final BookSide bids = new BookSide(Side.BUY);
    private final BookSide asks = new BookSide(Side.SELL);
    private final Map<Long, LimitOrder> orderIndex = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    BookSide sideFor(Side side){
        return side == Side.BUY ? bids : asks;
    }

    BookSide opposite(Side side){
        return side == Side.BUY ? asks : bids;
    }

    void addOrder(LimitOrder order){
        sideFor(order.getSide()).add(order);
        orderIndex.put(order.getId(), order);
    }

    /** Removes a resting order (fully filled or cancelled) from the book and the index. */
    void removeOrder(LimitOrder order){
        sideFor(order.getSide()).remove(order);
        orderIndex.remove(order.getId());
    }

    /** Lowers the remaining quantity of a resting order, keeping level totals correct. */
    void reduceResting(LimitOrder order, int quantity){
        order.level.reduce(order, quantity);
    }

    boolean hasOrder(long id){
        return orderIndex.containsKey(id);
    }

    LimitOrder findOrder(long id){
        return orderIndex.get(id);
    }

    /**
     * Cancels a resting order. O(1), plus O(log P) when it was the last order at its price.
     *
     * @return true if an order was cancelled, false if the id is not resting
     */
    public boolean cancelOrder(long id){
        LimitOrder order = orderIndex.get(id);
        if(order == null) return false;

        removeOrder(order);
        return true;
    }

    public double getBestBid(){
        return bids.isEmpty() ? 0.0 : bids.bestLevel().price();
    }

    public double getBestAsk(){
        return asks.isEmpty() ? 0.0 : asks.bestLevel().price();
    }

    public LimitOrder getBestBidOrder(){
        return bids.bestOrder();
    }

    public LimitOrder getBestAskOrder(){
        return asks.bestOrder();
    }

    void recordTrade(Trade trade){
        trades.add(trade);
    }

    public void printOrderBook(){
        System.out.println("Side | Quantity | Price");
        printSide(bids);
        printSide(asks);
    }

    private void printSide(BookSide side){
        for(PriceLevel level : side.levels()){
            for(LimitOrder order = level.head(); order != null; order = order.next){
                System.out.println(order.getSide() + "|\t" + order.getQuantity() + "|\t" + level.price());
            }
        }
    }

    public void printTrades(){
        if(trades.isEmpty()) return;

        for(Trade trade : trades){
            System.out.println("TRADE -> Quantity: " + trade.getQuantity() + ", Price: " + trade.getPrice());
        }
    }

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }
}
