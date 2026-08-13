package orderbook;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.Throughput,
        Mode.AverageTime
})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class MatchingEngineBenchmark {

    @State(Scope.Thread)
    public static class BenchmarkState {

        OrderBook orderBook;
        MatchingEngine engine;

        @Setup(Level.Iteration)
        public void setup() {
            orderBook = new OrderBook();
            engine = new MatchingEngine(orderBook);

            /*
             * Create liquidity across multiple price levels.
             *
             * The setup happens outside the benchmark measurement.
             */
            for (int i = 0; i < 100; i++) {
                engine.processOrder(
                        new LimitOrder(
                                i,
                                Side.SELL,
                                100,
                                100.0 + i
                        )
                );
            }
        }
    }

    @Benchmark
    public void marketBuyMatching(BenchmarkState state) {
        state.engine.processOrder(
                new MarketOrder(
                        1000,
                        Side.BUY,
                        500
                )
        );
    }
}