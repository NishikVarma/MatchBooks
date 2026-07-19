package orderbook;

import java.util.*;

public class OrderBook {
    private final TreeMap<Double, Queue<Order>> bidOffers;
    private final TreeMap<Double, Queue<Order>> askOffers;
    private final Map<Long, Order> orderIndex = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    public OrderBook(){
        bidOffers = new TreeMap<>(Comparator.reverseOrder());
        askOffers = new TreeMap<>();
    }

    void addOrder(Order order){
        Side side = order.getSide();
        double price = order.getPrice();
        orderIndex.put(order.getId(), order);
        TreeMap<Double, Queue<Order>> book;

        if(side == Side.BUY){
            book = bidOffers;
        }else{
            book = askOffers;
        }

        if(!book.containsKey(price)){
            book.put(price, new LinkedList<>());
        }

        book.get(price).offer(order);
    }

    public double getBestBid(){
        return bidOffers.isEmpty() ? 0.0 : bidOffers.firstKey();
    }

    public double getBestAsk(){
        return askOffers.isEmpty() ? 0.0 : askOffers.firstKey();
    }

    boolean canBuyOrderMatch(Order order){
        return !askOffers.isEmpty() && order.getPrice() >= getBestAsk();
    }

    boolean canSellOrderMatch(Order order){
        return !bidOffers.isEmpty() && order.getPrice() <= getBestBid();
    }

    public Order getBestBidOrder(){
        if(bidOffers.isEmpty()) return null;

        return bidOffers.firstEntry().getValue().peek();
    }

    public Order getBestAskOrder(){
        if(askOffers.isEmpty()) return null;

        return askOffers.firstEntry().getValue().peek();
    }

    void removeBestBidOrder(){
        removeBestOrder(bidOffers);
    }

    void removeBestAskOrder(){
        removeBestOrder(askOffers);
    }

    private void removeBestOrder(TreeMap<Double, Queue<Order>> book){
        double bestPrice = book.firstKey();
        Queue<Order> ordersAtBestPrice = book.get(bestPrice);

        Order order = ordersAtBestPrice.peek();
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

        TreeMap<Double, Queue<Order>> book;

        if(order.getSide() == Side.BUY){
            book = bidOffers;
        }else{
            book = askOffers;
        }

        Queue<Order> orders = book.get(order.getPrice());
        orders.remove(order);
        if(orders.isEmpty()){
            book.remove(order.getPrice());
        }

        orderIndex.remove(id);
    }

    void recordTrade(Trade trade){
        trades.add(trade);
    }

    public void printOrderBook(){
        System.out.println("main.java.orderbook.Side | Quantity | Price");
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
