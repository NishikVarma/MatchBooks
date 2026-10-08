package orderbook;
import java.time.Instant;

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

    public void processOrder(Order order) {
        if(order.isLimitOrder() && order.getTimeInForce() == TimeInForce.FOK){
            LimitOrder limitOrder = (LimitOrder) order;
            if(!orderFillable(limitOrder)) return;
        }

        if (order.getSide() == Side.BUY) {
            processBuyOrder(order);
        } else {
            processSellOrder(order);
        }
    }

    void processBuyOrder(Order incomingOrder){
        while(incomingOrder.getQuantity() > 0 && orderBook.canBuyOrderMatch(incomingOrder)){
            LimitOrder bestAsk = orderBook.getBestAskOrder();

            Trade trade = executeMatch(incomingOrder, bestAsk);
            orderBook.recordTrade(trade);

            if(bestAsk.getQuantity() == 0){
                orderBook.removeBestAskOrder();
            }
        }

        if(incomingOrder.isLimitOrder() && incomingOrder.getQuantity() > 0){
            if(incomingOrder.getTimeInForce() == TimeInForce.GTC){
                LimitOrder limitOrder = (LimitOrder) incomingOrder;
                orderBook.addOrder(limitOrder);
            }
        }
    }

    void processSellOrder(Order incomingOrder){
        while(incomingOrder.getQuantity() > 0 && orderBook.canSellOrderMatch(incomingOrder)){
            LimitOrder bestBid = orderBook.getBestBidOrder();

            Trade trade = executeMatch(incomingOrder, bestBid);
            orderBook.recordTrade(trade);

            if(bestBid.getQuantity() == 0){
                orderBook.removeBestBidOrder();
            }
        }

        if(incomingOrder.isLimitOrder() && incomingOrder.getQuantity() > 0){
            if(incomingOrder.getTimeInForce() == TimeInForce.GTC){
                LimitOrder limitOrder = (LimitOrder) incomingOrder;
                orderBook.addOrder(limitOrder);
            }
        }
    }

    private boolean orderFillable(LimitOrder order){
        if(order.getSide() == Side.BUY){
            return orderBook.canBuyOrderFill(order);
        }else{
            return orderBook.canSellOrderFill(order);
        }
    }
}
