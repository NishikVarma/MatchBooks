package orderbook;

import java.util.*;

public class OrderBook {
    private final TreeMap<Double, Queue<LimitOrder>> bidOffers;
    private final TreeMap<Double, Queue<LimitOrder>> askOffers;
    private final Map<Long, Order> orderIndex = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    public OrderBook(){
        bidOffers = new TreeMap<>(Comparator.reverseOrder());
        askOffers = new TreeMap<>();
    }

    void addOrder(LimitOrder order){
        Side side = order.getSide();
        double price = order.getPrice();
        orderIndex.put(order.getId(), order);
        TreeMap<Double, Queue<LimitOrder>> book;

        if(side == Side.BUY){
            book = bidOffers;
        }else{
            book = askOffers;
        }

        book.computeIfAbsent(price, p -> new LinkedList<>());
        book.get(price).offer(order);
    }

    boolean hasOrder(long id){
        return orderIndex.containsKey(id);
    }

    public double getBestBid(){
        return bidOffers.isEmpty() ? 0.0 : bidOffers.firstKey();
    }

    public double getBestAsk(){
        return askOffers.isEmpty() ? 0.0 : askOffers.firstKey();
    }

    boolean canBuyOrderMatch(Order order){
        if(order.isMarketOrder()){
            return !askOffers.isEmpty();
        }

        return !askOffers.isEmpty() && ((LimitOrder) order).getPrice() >= getBestAsk();
    }

    boolean canSellOrderMatch(Order order){
        if(order.isMarketOrder()){
            return !bidOffers.isEmpty();
        }

        return !bidOffers.isEmpty() && ((LimitOrder) order).getPrice() <= getBestBid();
    }

    public LimitOrder getBestBidOrder(){
        if(bidOffers.isEmpty()) return null;

        return bidOffers.firstEntry().getValue().peek();
    }

    public LimitOrder getBestAskOrder(){
        if(askOffers.isEmpty()) return null;

        return askOffers.firstEntry().getValue().peek();
    }

    void removeBestBidOrder(){
        removeBestOrder(bidOffers);
    }

    void removeBestAskOrder(){
        removeBestOrder(askOffers);
    }

    private void removeBestOrder(TreeMap<Double, Queue<LimitOrder>> book){
        double bestPrice = book.firstKey();
        Queue<LimitOrder> ordersAtBestPrice = book.get(bestPrice);

        LimitOrder order = ordersAtBestPrice.peek();
        if(order.getQuantity() == 0){
            orderIndex.remove(order.getId());
            ordersAtBestPrice.poll();
            if(ordersAtBestPrice.isEmpty()){
                book.remove(bestPrice);
            }
        }
    }

    public void cancelOrder(long id){
        Order order = orderIndex.get(id);

        if(order == null) return;
        if(order.getOrderType() != OrderType.LIMIT) return;

        LimitOrder limitOrder = (LimitOrder) order;

        TreeMap<Double, Queue<LimitOrder>> book;
        if(order.getSide() == Side.BUY){
            book = bidOffers;
        }else{
            book = askOffers;
        }

        Queue<LimitOrder> orders = book.get(limitOrder.getPrice());
        orders.remove(order);
        if(orders.isEmpty()){
            book.remove(limitOrder.getPrice());
        }

        orderIndex.remove(id);
    }

    boolean canBuyOrderFill(LimitOrder incomingOrder){
        int totalQuantity = 0;

        for(Map.Entry<Double, Queue<LimitOrder>> entry : askOffers.entrySet()){
            if(entry.getKey() > incomingOrder.getPrice()) break;
            for(LimitOrder limitOrder : entry.getValue()){
                totalQuantity += limitOrder.getQuantity();

                if(totalQuantity >= incomingOrder.getQuantity()){
                    return true;
                }
            }
        }

        return false;
    }

    boolean canSellOrderFill(LimitOrder incomingOrder){
        int totalQuantity = 0;

        for(Map.Entry<Double, Queue<LimitOrder>> entry : bidOffers.entrySet()){
            if(entry.getKey() < incomingOrder.getPrice()) break;
            for(LimitOrder limitOrder : entry.getValue()){
                totalQuantity += limitOrder.getQuantity();

                if(totalQuantity >= incomingOrder.getQuantity()){
                    return true;
                }
            }
        }

        return false;
    }

    void recordTrade(Trade trade){
        trades.add(trade);
    }

    public void printOrderBook(){
        System.out.println("main.java.orderbook.Side | Quantity | Price");
        for(Map.Entry<Double, Queue<LimitOrder>> bidOrder : bidOffers.entrySet()){
            for(LimitOrder order : bidOrder.getValue()){
                System.out.println(order.getSide() + "|\t" + order.getQuantity() + "|\t" + bidOrder.getKey());
            }
        }

        for(Map.Entry<Double, Queue<LimitOrder>> askOrder : askOffers.entrySet()){
            for(LimitOrder order : askOrder.getValue()){
                System.out.println(order.getSide() + "|\t" + order.getQuantity() + "|\t" + askOrder.getKey());
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
