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

        int incomingQuantity = incomingOrder.getQuantity();
        int existingQuantity = existingOrder.getQuantity();

        int tradeQuantity = Math.min(incomingQuantity, existingQuantity);

        incomingOrder.setQuantity(incomingQuantity - tradeQuantity);
        existingOrder.setQuantity(existingQuantity - tradeQuantity);

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

        if(order.isLimitOrder() && order.getTimeInForce() == TimeInForce.FOK){
            LimitOrder limitOrder = (LimitOrder) order;
            if(!orderFillable(limitOrder)){
                return new ExecutionResult(OrderStatus.CANCELLED, 0, submittedQuantity, List.of(), null);
            }
        }

        List<Trade> trades = new ArrayList<>();
        boolean rested;

        if (order.getSide() == Side.BUY) {
            rested = processBuyOrder(order, trades);
        } else {
            rested = processSellOrder(order, trades);
        }

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

    private RejectReason validate(Order order){
        if(order.getSide() == null) return RejectReason.INVALID_SIDE;
        if(order.getTimeInForce() == null) return RejectReason.MISSING_TIME_IN_FORCE;
        if(order.getQuantity() <= 0) return RejectReason.INVALID_QUANTITY;

        if(order.isLimitOrder()){
            double price = ((LimitOrder) order).getPrice();
            if(!Double.isFinite(price) || price <= 0) return RejectReason.INVALID_PRICE;
        }

        if(orderBook.hasOrder(order.getId())) return RejectReason.DUPLICATE_ID;

        return null;
    }

    /** @return true if the unfilled remainder was placed in the book */
    boolean processBuyOrder(Order incomingOrder, List<Trade> trades){
        while(incomingOrder.getQuantity() > 0 && orderBook.canBuyOrderMatch(incomingOrder)){
            LimitOrder bestAsk = orderBook.getBestAskOrder();

            Trade trade = executeMatch(incomingOrder, bestAsk);
            orderBook.recordTrade(trade);
            trades.add(trade);

            if(bestAsk.getQuantity() == 0){
                orderBook.removeBestAskOrder();
            }
        }

        return restRemainder(incomingOrder);
    }

    /** @return true if the unfilled remainder was placed in the book */
    boolean processSellOrder(Order incomingOrder, List<Trade> trades){
        while(incomingOrder.getQuantity() > 0 && orderBook.canSellOrderMatch(incomingOrder)){
            LimitOrder bestBid = orderBook.getBestBidOrder();

            Trade trade = executeMatch(incomingOrder, bestBid);
            orderBook.recordTrade(trade);
            trades.add(trade);

            if(bestBid.getQuantity() == 0){
                orderBook.removeBestBidOrder();
            }
        }

        return restRemainder(incomingOrder);
    }

    private boolean restRemainder(Order order){
        if(order.isLimitOrder() && order.getQuantity() > 0 && order.getTimeInForce() == TimeInForce.GTC){
            orderBook.addOrder((LimitOrder) order);
            return true;
        }
        return false;
    }

    private boolean orderFillable(LimitOrder order){
        if(order.getSide() == Side.BUY){
            return orderBook.canBuyOrderFill(order);
        }else{
            return orderBook.canSellOrderFill(order);
        }
    }
}
