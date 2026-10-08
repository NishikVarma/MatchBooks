package orderbook;

/**
 * All resting orders at one price, oldest first (ADR-0029).
 *
 * <p>An intrusive doubly linked list: the {@link LimitOrder}s themselves are the
 * nodes, so appending, removing from the middle (cancel) and removing the head
 * (fill) are all O(1). The level also keeps its order count and total remaining
 * quantity up to date, so the fill-or-kill check never has to walk orders.
 */
final class PriceLevel {

    private final long price;
    private LimitOrder head;
    private LimitOrder tail;
    private int orderCount;
    private long totalQuantity;

    PriceLevel(long price) {
        this.price = price;
    }

    long price() {
        return price;
    }

    LimitOrder head() {
        return head;
    }

    boolean isEmpty() {
        return head == null;
    }

    int orderCount() {
        return orderCount;
    }

    long totalQuantity() {
        return totalQuantity;
    }

    /** Puts the order at the back of the line (lowest time priority). */
    void append(LimitOrder order) {
        order.prev = tail;
        order.next = null;
        order.level = this;

        if (tail == null) {
            head = order;
        } else {
            tail.next = order;
        }
        tail = order;

        orderCount++;
        totalQuantity += order.getQuantity();
    }

    /** Removes the order from wherever it is in the line. */
    void unlink(LimitOrder order) {
        if (order.prev == null) {
            head = order.next;
        } else {
            order.prev.next = order.next;
        }

        if (order.next == null) {
            tail = order.prev;
        } else {
            order.next.prev = order.prev;
        }

        order.prev = null;
        order.next = null;
        order.level = null;

        orderCount--;
        totalQuantity -= order.getQuantity();
    }

    /** Lowers a resting order's quantity and the level total together. */
    void reduce(LimitOrder order, int quantity) {
        order.reduceQuantity(quantity);
        totalQuantity -= quantity;
    }
}
