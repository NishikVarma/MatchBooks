import java.util.*;

public class OrderBook {
    TreeMap<Double, Queue<Order>> bidOffers;
    TreeMap<Double, Queue<Order>> askOffers;

    OrderBook(){
        bidOffers = new TreeMap<>(Comparator.reverseOrder());
        askOffers = new TreeMap<>();
    }

    void addOrder(Order order){
        Side side = order.getSide();
        double price = order.getPrice();

        if(side == Side.BUY){
            if(!bidOffers.containsKey(price)){
                bidOffers.put(price, new LinkedList<>());
            }

            bidOffers.get(price).offer(order);
        }else{
            if(!askOffers.containsKey((price))){
                askOffers.put(price, new LinkedList<>());
            }

            askOffers.get(price).offer(order);
        }
    }

    double getBestBid(){
        if(bidOffers.isEmpty()) return 0.00;
        return bidOffers.firstKey();
    }

    double getBestAsk(){
        if(askOffers.isEmpty()) return 0.00;
        return askOffers.firstKey();
    }

    boolean canBuyOrderMatch(Order order){
        if(askOffers.isEmpty()) return false;

        double bidPrice = order.getPrice();
        double bestAsk = askOffers.firstKey();
        return bidPrice >= bestAsk;
    }

    boolean canSellOrderMatch(Order order){
        if(bidOffers.isEmpty()) return false;

        double askPrice = order.getPrice();
        double bestBid = bidOffers.firstKey();
        return askPrice <= bestBid;
    }

    Order getBestAskOrder(){
        if(askOffers.isEmpty()) return null;

        double bestAsk = askOffers.firstKey();
        Queue<Order> ordersAtBestAsk = askOffers.get(bestAsk);
        return ordersAtBestAsk.peek();

    }

    Order getBestBidOrder(){
        if(bidOffers.isEmpty()) return null;

        double bestBid = bidOffers.firstKey();
        Queue<Order> ordersAtBestBid = bidOffers.get(bestBid);
        return ordersAtBestBid.peek();
    }

    Trade executeTrade(Order incomingOrder, Order existingOrder){
        int incomingQuantity = incomingOrder.getQuantity();
        int existingQuantity = existingOrder.getQuantity();

        int tradeQuantity = Math.min(incomingQuantity, existingQuantity);
        incomingOrder.setQuantity(incomingQuantity - tradeQuantity);
        existingOrder.setQuantity(existingQuantity - tradeQuantity);

        return new Trade(existingOrder.getPrice(), tradeQuantity);
    }

    void removeBestAskOrder(){
        double bestAsk = askOffers.firstKey();
        Queue<Order> ordersAtBestAsk = askOffers.get(bestAsk);

        if(ordersAtBestAsk.peek().getQuantity() == 0){
            ordersAtBestAsk.poll();
            if(ordersAtBestAsk.isEmpty()){
                askOffers.remove(bestAsk);
            }
        }
    }

    void removeBestBidOrder(){
        double bestBid = bidOffers.firstKey();
        Queue<Order> ordersAtBestBid = bidOffers.get(bestBid);

        if(ordersAtBestBid.peek().getQuantity() == 0){
            ordersAtBestBid.poll();
            if(ordersAtBestBid.isEmpty()){
                bidOffers.remove(bestBid);
            }
        }
    }

    void processBuyOrder(Order incomingOrder){
        while(incomingOrder.getQuantity() > 0 &&  canBuyOrderMatch(incomingOrder)){
            Order bestAskOrder = getBestAskOrder();

            Trade trade = executeTrade(incomingOrder, bestAskOrder);
            System.out.println("TRADE -> Quantity: " + trade.getQuantity() + ", Price: " + trade.getPrice());
            if(bestAskOrder.getQuantity() == 0){
                removeBestAskOrder();
            }
        }

        if(incomingOrder.getQuantity() > 0){
            addOrder(incomingOrder);
        }
    }

    void processSellOrder(Order incomingOrder){
        while(incomingOrder.getQuantity() > 0 && canSellOrderMatch(incomingOrder)){
            Order bestBidOffer = getBestBidOrder();

            Trade trade = executeTrade(incomingOrder, bestBidOffer);
            System.out.println("TRADE -> Quantity: " + trade.getQuantity() + ", Price: " + trade.getPrice());
            
            if(bestBidOffer.getQuantity() == 0){
                removeBestBidOrder();
            }
        }

        if(incomingOrder.getQuantity() > 0){
            addOrder(incomingOrder);
        }
    }

    void processOrder(Order order){
        if(order.getSide() == Side.BUY){
            processBuyOrder(order);
        }else{
            processSellOrder(order);
        }
    }

    void printOrderBook(){
        System.out.println("Side | Quantity | Price");
        for(Map.Entry<Double, Queue<Order>> bidOrder : bidOffers.entrySet()){
            for(Order order : bidOrder.getValue()){
                System.out.println(order.getSide() + "|\t" + order.getQuantity() + "|\t" + bidOrder.getKey());
            }
        }

        for(Map.Entry<Double, Queue<Order>> askOrder : askOffers.entrySet()){
            for(Order order : askOrder.getValue()){
                System.out.println(order.getSide() + "|\t" + order.getQuantity() + "|\t" + askOrder.getKey());
            }
        }
    }
}
