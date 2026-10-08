package orderbook;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MatchingEngine {
    private final OrderBook orderBook;

    public MatchingEngine(OrderBook orderBook){
        this.orderBook = orderBook;
    }

    Trade executeMatch(Order incomingOrder, LimitOrder existingOrder) {

        long buyOrderId;
        long sellOrderId;

        int tradeQuantity = Math.min(incomingOrder.getQuantity(), existingOrder.getQuantity());

        incomingOrder.reduceQuantity(tradeQuantity);
        orderBook.reduceResting(existingOrder, tradeQuantity);

        final Instant timestamp = Instant.now();

        if (incomingOrder.getSide() == Side.BUY) {

            buyOrderId = incomingOrder.getId();
            sellOrderId = existingOrder.getId();

        } else {

            buyOrderId = existingOrder.getId();
            sellOrderId = incomingOrder.getId();
        }

        return new Trade(
                existingOrder.getPrice(),
                tradeQuantity,
                buyOrderId,
                sellOrderId,
                timestamp
        );
    }

    public ExecutionResult processOrder(Order order) {
        Objects.requireNonNull(order, "order");

        int submittedQuantity = order.getQuantity();

        RejectReason rejectReason = validate(order);
        if(rejectReason != null){
            return ExecutionResult.rejected(submittedQuantity, rejectReason);
        }

        if(order.getTimeInForce().requiresFullFill()){
            if(!orderFillable(order)){
                return new ExecutionResult(OrderStatus.CANCELLED, 0, submittedQuantity, List.of(), null);
            }
        }

        List<Trade> trades = new ArrayList<>();

        match(order, trades);
        boolean rested = restRemainder(order);

        int remaining = order.getQuantity();
        OrderStatus status;
        if(remaining == 0){
            status = OrderStatus.FILLED;
        }else if(rested){
            status = OrderStatus.RESTING;
        }else{
            status = OrderStatus.CANCELLED;
        }

        return new ExecutionResult(status, submittedQuantity - remaining, remaining, trades, null);
    }

    /** @return true if a resting order with this id was cancelled */
    public boolean cancelOrder(long id){
        return orderBook.cancelOrder(id);
    }

    /**
     * Amends a resting order (ADR-0031). Reducing the quantity at the same price keeps
     * time priority; any other change cancels the order and resubmits it at the back
     * of the line, where it may trade immediately.
     */
    public ExecutionResult replaceOrder(long id, int newQuantity, double newPrice){
        LimitOrder resting = orderBook.findOrder(id);
        if(resting == null){
            return ExecutionResult.rejected(newQuantity, RejectReason.UNKNOWN_ORDER);
        }
        if(newQuantity <= 0){
            return ExecutionResult.rejected(newQuantity, RejectReason.INVALID_QUANTITY);
        }
        if(!Double.isFinite(newPrice) || newPrice <= 0){
            return ExecutionResult.rejected(newQuantity, RejectReason.INVALID_PRICE);
        }

        if(newPrice == resting.getPrice() && newQuantity <= resting.getQuantity()){
            orderBook.reduceResting(resting, resting.getQuantity() - newQuantity);
            return new ExecutionResult(OrderStatus.RESTING, 0, newQuantity, List.of(), null);
        }

        orderBook.cancelOrder(id);
        return processOrder(new LimitOrder(id, resting.getSide(), newQuantity, newPrice, TimeInForce.GTC));
    }

    private RejectReason validate(Order order){
        if(order.getSide() == null) return RejectReason.INVALID_SIDE;
        if(order.getTimeInForce() == null) return RejectReason.MISSING_TIME_IN_FORCE;
        if(order.isMarketOrder() && order.getTimeInForce().restsRemainder()) return RejectReason.INVALID_TIME_IN_FORCE;
        if(order.getQuantity() <= 0) return RejectReason.INVALID_QUANTITY;

        if(order.isLimitOrder()){
            double price = ((LimitOrder) order).getPrice();
            if(!Double.isFinite(price) || price <= 0) return RejectReason.INVALID_PRICE;
        }

        if(orderBook.hasOrder(order.getId())) return RejectReason.DUPLICATE_ID;

        return null;
    }

    /** One matching loop for both sides (ADR-0028): hit the opposite side while prices cross. */
    private void match(Order incomingOrder, List<Trade> trades){
        BookSide opposite = orderBook.opposite(incomingOrder.getSide());

        while(incomingOrder.getQuantity() > 0 && opposite.crossedBy(incomingOrder)){
            LimitOrder resting = opposite.bestOrder();

            Trade trade = executeMatch(incomingOrder, resting);
            orderBook.recordTrade(trade);
            trades.add(trade);

            if(resting.getQuantity() == 0){
                orderBook.removeOrder(resting);
            }
        }
    }

    private boolean restRemainder(Order order){
        if(order.isLimitOrder() && order.getQuantity() > 0 && order.getTimeInForce().restsRemainder()){
            orderBook.addOrder((LimitOrder) order);
            return true;
        }
        return false;
    }

    private boolean orderFillable(Order order){
        return orderBook.opposite(order.getSide()).canFill(order);
    }
}
