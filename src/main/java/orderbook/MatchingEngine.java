package orderbook;
import java.time.Instant;

public class MatchingEngine {
    private final OrderBook orderBook;

    public MatchingEngine(OrderBook orderBook){
        this.orderBook = orderBook;
    }

    Trade executeMatch(LimitOrder incomingOrder, LimitOrder existingOrder){
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

        return new Trade(existingOrder.getPrice(), tradeQuantity, buyOrderId, sellOrderId, timestamp);
    }

    void processBuyOrder(LimitOrder incomingOrder){
        while(incomingOrder.getQuantity() > 0 && orderBook.canBuyOrderMatch(incomingOrder)){
            LimitOrder bestAsk = orderBook.getBestAskOrder();

            Trade trade = executeMatch(incomingOrder, bestAsk);
            orderBook.recordTrade(trade);

            if(bestAsk.getQuantity() == 0){
                orderBook.removeBestAskOrder();
            }
        }

        if(incomingOrder.getQuantity() > 0){
            orderBook.addOrder(incomingOrder);
        }
    }

    void processSellOrder(LimitOrder incomingOrder){
        while(incomingOrder.getQuantity() > 0 && orderBook.canSellOrderMatch(incomingOrder)){
            LimitOrder bestBid = orderBook.getBestBidOrder();

            Trade trade = executeMatch(incomingOrder, bestBid);
            orderBook.recordTrade(trade);

            if(bestBid.getQuantity() == 0){
                orderBook.removeBestBidOrder();
            }
        }

        if(incomingOrder.getQuantity() > 0){
            orderBook.addOrder(incomingOrder);
        }
    }

    public void processOrder(LimitOrder order){
        if(order.getSide() == Side.BUY){
            processBuyOrder(order);
        }else{
            processSellOrder(order);
        }
    }
}
