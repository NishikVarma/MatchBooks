package orderbook;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookPrintTest {

    private static String capture(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void printOrderBookListsBidsThenAsksUsingTheScaleForPrices() {
        PriceScale cents = new PriceScale(2);
        OrderBook book = new OrderBook(cents);
        MatchingEngine engine = new MatchingEngine(book);
        engine.processOrder(new LimitOrder(1, Side.BUY, 10, 9950));
        engine.processOrder(new LimitOrder(2, Side.SELL, 5, 10100));

        String out = capture(book::printOrderBook);

        assertEquals("Side | Quantity | Price\nBUY|\t10|\t99.50\nSELL|\t5|\t101.00\n", out);
    }

    @Test
    void printTradesShowsQuantityAndFormattedPrice() {
        OrderBook book = new OrderBook(new PriceScale(2));
        MatchingEngine engine = new MatchingEngine(book);
        engine.processOrder(new LimitOrder(1, Side.SELL, 5, 10100));
        engine.processOrder(new MarketOrder(2, Side.BUY, 5));

        assertEquals("TRADE -> Quantity: 5, Price: 101.00\n", capture(book::printTrades));
    }

    @Test
    void printTradesIsSilentWhenThereAreNoTrades() {
        assertEquals("", capture(new OrderBook()::printTrades));
    }
}
