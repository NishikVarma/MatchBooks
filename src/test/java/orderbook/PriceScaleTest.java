package orderbook;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/** Edge conversion (ADR-0032) and the empty-side accessors. */
class PriceScaleTest {

    @Test
    void parsesDecimalsExactly() {
        PriceScale cents = new PriceScale(2);

        assertEquals(10025, cents.toTicks("100.25"));
        assertEquals(10000, cents.toTicks("100"));
        assertEquals(10, cents.toTicks("0.10"));
        assertEquals(30, cents.toTicks("0.3"));          // the classic 0.1 + 0.2 trap has no equivalent here
    }

    @Test
    void rejectsPricesOffTheGrid() {
        PriceScale cents = new PriceScale(2);

        assertThrows(IllegalArgumentException.class, () -> cents.toTicks("100.255"));
        assertThrows(IllegalArgumentException.class, () -> cents.toTicks("99999999999999999999"));
        assertThrows(NumberFormatException.class, () -> cents.toTicks("abc"));
    }

    @Test
    void formatsTicksBackToDecimals() {
        PriceScale cents = new PriceScale(2);

        assertEquals("100.25", cents.format(10025));
        assertEquals("0.05", cents.format(5));
        assertEquals("7", PriceScale.TICKS.format(7));
    }

    @Test
    void rejectsSillyScales() {
        assertThrows(IllegalArgumentException.class, () -> new PriceScale(-1));
        assertThrows(IllegalArgumentException.class, () -> new PriceScale(19));
    }

    @Test
    void bestPriceOfAnEmptySideThrowsInsteadOfReturningAFakePrice() {
        OrderBook book = new OrderBook();

        assertFalse(book.hasBid());
        assertFalse(book.hasAsk());
        assertThrows(NoSuchElementException.class, book::bestBid);
        assertThrows(NoSuchElementException.class, book::bestAsk);
    }

    @Test
    void bookWorksEndToEndWithDecimalPrices() {
        PriceScale cents = new PriceScale(2);
        OrderBook book = new OrderBook(cents);
        MatchingEngine engine = new MatchingEngine(book);

        engine.processOrder(new LimitOrder(1, Side.SELL, 10, cents.toTicks("100.10")));
        ExecutionResult result =
                engine.processOrder(new LimitOrder(2, Side.BUY, 10, cents.toTicks("100.20")));

        assertEquals("100.10", cents.format(result.trades().get(0).getPrice()));
    }
}
