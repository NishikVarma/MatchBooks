package orderbook;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feeds the same random operations to the real engine and to a naive reference model
 * and compares everything after every step (ADR-0035).
 */
class RandomisedDifferentialTest {

    private static final int SEEDS = 60;
    private static final int OPERATIONS_PER_SEED = 1500;

    @Test
    void realEngineAgreesWithTheNaiveReferenceOnRandomStreams() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            runSeed(seed);
        }
    }

    private void runSeed(long seed) {
        Random random = new Random(seed);
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);
        ReferenceBook reference = new ReferenceBook();

        long nextId = 1;
        List<Long> issuedIds = new ArrayList<>();
        long lastTradeId = 0;

        for (int step = 1; step <= OPERATIONS_PER_SEED; step++) {
            String where = "seed " + seed + ", step " + step;
            int roll = random.nextInt(100);

            if (roll < 70) {
                // new order (sometimes re-using an id to provoke DUPLICATE_ID)
                long id = (!issuedIds.isEmpty() && random.nextInt(20) == 0)
                        ? issuedIds.get(random.nextInt(issuedIds.size()))
                        : nextId++;
                issuedIds.add(id);

                Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
                int quantity = 1 + random.nextInt(20);
                boolean market = random.nextInt(100) < 20;
                TimeInForce tif = pickTimeInForce(random, market);

                Order realOrder;
                RefOrder refOrder;
                if (market) {
                    realOrder = new MarketOrder(id, side, quantity, tif);
                    refOrder = new RefOrder(id, side, quantity, 0, true, tif);
                } else {
                    long price = 95 + random.nextInt(11);
                    realOrder = new LimitOrder(id, side, quantity, price, tif);
                    refOrder = new RefOrder(id, side, quantity, price, false, tif);
                }

                compare(where, reference.submit(refOrder), engine.processOrder(realOrder));

            } else if (roll < 85) {
                long id = pickId(random, issuedIds, nextId);
                assertEquals(reference.cancel(id), engine.cancelOrder(id), where + " cancel " + id);

            } else {
                long id = pickId(random, issuedIds, nextId);
                int quantity = random.nextInt(25) - 2;           // includes invalid values
                long price = 94 + random.nextInt(13);
                compare(where, reference.replace(id, quantity, price), engine.replaceOrder(id, quantity, price));
            }

            assertEquals(reference.depth(), toMap(book.depth(Integer.MAX_VALUE)), where + " depth");
            if (book.hasBid() && book.hasAsk()) {
                assertTrue(book.bestBid() < book.bestAsk(), where + " book is crossed");
            }
            for (Trade trade : book.getTrades().subList((int) lastTradeId, book.getTrades().size())) {
                assertEquals(lastTradeId + 1, trade.getTradeId(), where + " trade id sequence");
                lastTradeId++;
            }
        }
    }

    private static TimeInForce pickTimeInForce(Random random, boolean market) {
        int r = random.nextInt(100);
        if (market) {
            return r < 10 ? TimeInForce.GTC : (r < 70 ? TimeInForce.IOC : TimeInForce.FOK);   // GTC is rejected
        }
        return r < 65 ? TimeInForce.GTC : (r < 82 ? TimeInForce.IOC : TimeInForce.FOK);
    }

    private static long pickId(Random random, List<Long> issued, long nextId) {
        if (issued.isEmpty() || random.nextInt(10) == 0) return nextId + 1000;   // unknown id
        return issued.get(random.nextInt(issued.size()));
    }

    private static void compare(String where, RefResult expected, ExecutionResult actual) {
        assertEquals(expected.status, actual.status(), where + " status");
        assertEquals(expected.filled, actual.filledQuantity(), where + " filled");
        assertEquals(expected.remaining, actual.remainingQuantity(), where + " remaining");
        assertEquals(expected.reason, actual.rejectReason(), where + " reason");
        assertEquals(expected.trades.size(), actual.trades().size(), where + " trade count");
        for (int i = 0; i < expected.trades.size(); i++) {
            RefTrade e = expected.trades.get(i);
            Trade a = actual.trades().get(i);
            assertEquals(e.price, a.getPrice(), where + " trade " + i + " price");
            assertEquals(e.quantity, a.getQuantity(), where + " trade " + i + " quantity");
            assertEquals(e.buyId, a.getBuyOrderId(), where + " trade " + i + " buy id");
            assertEquals(e.sellId, a.getSellOrderId(), where + " trade " + i + " sell id");
            assertEquals(e.aggressor, a.getAggressorSide(), where + " trade " + i + " aggressor");
        }
    }

    /** Both sides flattened to "BUY@99" -> {quantity, count}, so one map comparison checks everything. */
    private static TreeMap<String, String> toMap(DepthSnapshot depth) {
        TreeMap<String, String> map = new TreeMap<>();
        for (LevelView v : depth.bids()) map.put("BUY@" + v.price(), v.quantity() + "x" + v.orderCount());
        for (LevelView v : depth.asks()) map.put("SELL@" + v.price(), v.quantity() + "x" + v.orderCount());
        return map;
    }

    // ======================================================================
    // Reference model: obviously correct, deliberately slow. Shares no code with the engine.
    // ======================================================================

    private record RefTrade(long price, int quantity, long buyId, long sellId, Side aggressor) {
    }

    private record RefResult(OrderStatus status, int filled, int remaining, RejectReason reason,
                             List<RefTrade> trades) {
    }

    private static final class RefOrder {
        final long id;
        final Side side;
        int quantity;
        final long price;
        final boolean market;
        final TimeInForce tif;

        RefOrder(long id, Side side, int quantity, long price, boolean market, TimeInForce tif) {
            this.id = id;
            this.side = side;
            this.quantity = quantity;
            this.price = price;
            this.market = market;
            this.tif = tif;
        }
    }

    private static final class ReferenceBook {
        private final List<RefOrder> bids = new ArrayList<>();   // arrival order
        private final List<RefOrder> asks = new ArrayList<>();

        RefResult submit(RefOrder order) {
            int submitted = order.quantity;
            if (order.quantity <= 0) return reject(submitted, RejectReason.INVALID_QUANTITY);
            if (order.market && order.tif == TimeInForce.GTC) return reject(submitted, RejectReason.INVALID_TIME_IN_FORCE);
            if (!order.market && order.price <= 0) return reject(submitted, RejectReason.INVALID_PRICE);
            if (find(order.id) != null) return reject(submitted, RejectReason.DUPLICATE_ID);

            List<RefOrder> opposite = order.side == Side.BUY ? asks : bids;

            if (order.tif == TimeInForce.FOK) {
                long available = 0;
                for (RefOrder r : opposite) {
                    if (acceptable(order, r.price)) available += r.quantity;
                }
                if (available < order.quantity) {
                    return new RefResult(OrderStatus.CANCELLED, 0, submitted, null, List.of());
                }
            }

            List<RefTrade> trades = new ArrayList<>();
            while (order.quantity > 0) {
                RefOrder best = bestOf(opposite);
                if (best == null || !acceptable(order, best.price)) break;

                int qty = Math.min(order.quantity, best.quantity);
                order.quantity -= qty;
                best.quantity -= qty;
                boolean buyerIsIncoming = order.side == Side.BUY;
                trades.add(new RefTrade(best.price, qty,
                        buyerIsIncoming ? order.id : best.id,
                        buyerIsIncoming ? best.id : order.id,
                        order.side));
                if (best.quantity == 0) opposite.remove(best);
            }

            boolean rested = false;
            if (order.quantity > 0 && !order.market && order.tif == TimeInForce.GTC) {
                (order.side == Side.BUY ? bids : asks).add(order);
                rested = true;
            }

            OrderStatus status = order.quantity == 0 ? OrderStatus.FILLED
                    : rested ? OrderStatus.RESTING : OrderStatus.CANCELLED;
            return new RefResult(status, submitted - order.quantity, order.quantity, null, trades);
        }

        boolean cancel(long id) {
            RefOrder r = find(id);
            if (r == null) return false;
            (r.side == Side.BUY ? bids : asks).remove(r);
            return true;
        }

        RefResult replace(long id, int newQuantity, long newPrice) {
            RefOrder r = find(id);
            if (r == null) return reject(newQuantity, RejectReason.UNKNOWN_ORDER);
            if (newQuantity <= 0) return reject(newQuantity, RejectReason.INVALID_QUANTITY);
            if (newPrice <= 0) return reject(newQuantity, RejectReason.INVALID_PRICE);

            if (newPrice == r.price && newQuantity <= r.quantity) {
                r.quantity = newQuantity;                         // keeps its place in line
                return new RefResult(OrderStatus.RESTING, 0, newQuantity, null, List.of());
            }

            cancel(id);
            return submit(new RefOrder(id, r.side, newQuantity, newPrice, false, TimeInForce.GTC));
        }

        /** "SIDE@price" -> "quantityXcount", the same shape as the real snapshot. */
        TreeMap<String, String> depth() {
            TreeMap<String, String> map = new TreeMap<>();
            add(map, bids, "BUY@");
            add(map, asks, "SELL@");
            return map;
        }

        private static void add(TreeMap<String, String> map, List<RefOrder> orders, String prefix) {
            TreeMap<String, long[]> sums = new TreeMap<>();
            for (RefOrder o : orders) {
                long[] cell = sums.computeIfAbsent(prefix + o.price, k -> new long[2]);
                cell[0] += o.quantity;
                cell[1] += 1;
            }
            sums.forEach((k, v) -> map.put(k, v[0] + "x" + v[1]));
        }

        private RefOrder find(long id) {
            for (RefOrder o : bids) if (o.id == id) return o;
            for (RefOrder o : asks) if (o.id == id) return o;
            return null;
        }

        /** Best price first, then oldest (list order is arrival order). */
        private RefOrder bestOf(List<RefOrder> side) {
            RefOrder best = null;
            for (RefOrder o : side) {
                if (best == null) {
                    best = o;
                } else if (o.side == Side.SELL ? o.price < best.price : o.price > best.price) {
                    best = o;
                }
            }
            return best;
        }

        private static boolean acceptable(RefOrder incoming, long restingPrice) {
            if (incoming.market) return true;
            return incoming.side == Side.BUY ? incoming.price >= restingPrice : incoming.price <= restingPrice;
        }

        private static RefResult reject(int quantity, RejectReason reason) {
            return new RefResult(OrderStatus.REJECTED, 0, quantity, reason, List.of());
        }
    }
}
