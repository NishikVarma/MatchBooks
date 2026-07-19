# Java Limit Order Book Matching Engine

A production-oriented Java implementation of an in-memory limit order book matching engine that follows **price-time priority** for matching BUY and SELL limit orders.

The goal of this project is to understand how modern electronic exchanges work by implementing the core matching engine from scratch while learning backend engineering, data structures, clean architecture, and system design.

---

## Features

### Implemented

- ✅ BUY and SELL limit orders
- ✅ Price-time priority matching
- ✅ FIFO execution within each price level
- ✅ Partial order fills
- ✅ Full order fills
- ✅ Order cancellation
- ✅ Order lookup by ID
- ✅ Trade generation
- ✅ Trade history
- ✅ Trade metadata
    - Buy Order ID
    - Sell Order ID
    - Execution timestamp
- ✅ Order types
    - LIMIT
    - MARKET *(model introduced, execution coming next)*
- ✅ JUnit 5 test suite

---

## Matching Rules

### Price Priority

- BUY orders match the **lowest available SELL price**.
- SELL orders match the **highest available BUY price**.

Example:

```
SELL 50 @ 100
SELL 30 @ 101

BUY 60 @ 101
```

Execution:

```
BUY 60
├── 50 @ 100
└── 10 @ 101
```

Remaining book:

```
SELL 20 @ 101
```

---

### Time Priority (FIFO)

Orders at the same price level are matched in the order they were received.

Example:

```
SELL 20 @ 100
SELL 30 @ 100
SELL 40 @ 100

BUY 35 @ 100
```

Execution:

```
SELL 20 -> Fully Executed
SELL 30 -> Partially Executed (15 remaining)
SELL 40 -> Unchanged
```

Remaining:

```
SELL 15 @ 100
SELL 40 @ 100
```

---

## Architecture

```
        Main
          │
          ▼
   MatchingEngine
          │
          ▼
      OrderBook
```

### MatchingEngine

Responsible for:

- Processing incoming orders
- Executing matches
- Applying price-time priority
- Creating trades

### OrderBook

Responsible for:

- Maintaining BUY and SELL books
- Managing price levels
- Order lookup
- Order cancellation
- Trade history

---

## Data Structures

```text
TreeMap<Double, Queue<Order>> bidBook
TreeMap<Double, Queue<Order>> askBook

HashMap<Long, Order> orderIndex

ArrayList<Trade> trades
```

### Why these structures?

**TreeMap**

- Maintains sorted price levels
- BUY book uses reverse ordering
- SELL book uses natural ordering

**Queue**

- Preserves FIFO execution within a price level

**HashMap**

- Enables O(1) average order lookup by ID

**ArrayList**

- Stores execution history

---

## Project Structure

```
src/
├── main/
│   └── java/
│       └── orderbook/
│           ├── Main.java
│           ├── MatchingEngine.java
│           ├── Order.java
│           ├── OrderBook.java
│           ├── OrderType.java
│           ├── Side.java
│           └── Trade.java
│
└── test/
    └── java/
        └── orderbook/
            └── MatchingEngineTest.java
```

---

## Domain Model

### Order

Represents an incoming order.

Fields:

- Order ID
- Side (BUY / SELL)
- Quantity
- Price
- Order Type

---

### Trade

Represents a completed execution.

Fields:

- Buy Order ID
- Sell Order ID
- Execution Price
- Executed Quantity
- Execution Timestamp

---

## Time Complexity

| Operation | Complexity |
|-----------|-----------:|
| Add Order | O(log n) |
| Cancel Order | O(1) average |
| Best Bid Lookup | O(1) |
| Best Ask Lookup | O(1) |
| Match Best Order | O(1) per fill |
| Remove Empty Price Level | O(log n) |

Where **n** is the number of price levels.

---

## Example

```java
OrderBook orderBook = new OrderBook();
MatchingEngine engine = new MatchingEngine(orderBook);

engine.processOrder(
    new Order(
        1,
        Side.SELL,
        20,
        100.0,
        OrderType.LIMIT
    )
);

engine.processOrder(
    new Order(
        2,
        Side.BUY,
        20,
        100.0,
        OrderType.LIMIT
    )
);

orderBook.printTrades();
```

Example output:

```
TRADE
Buy Order : 2
Sell Order: 1
Quantity  : 20
Price     : 100.0
```

---

## Testing

The project uses **JUnit 5**.

Current test coverage includes:

- Single order insertion
- Full order match
- Partial fill
- FIFO execution
- Order cancellation
- No-match scenarios
- Trade generation

Run tests with:

```bash
mvn test
```

---

## Roadmap

### Matching Engine

- [x] Limit orders
- [x] FIFO matching
- [x] Partial fills
- [x] Order cancellation
- [x] Trade history
- [x] Order types
- [ ] Market order execution
- [ ] IOC orders
- [ ] FOK orders

### Performance

- [ ] Benchmarking
- [ ] JMH benchmarks
- [ ] Throughput analysis
- [ ] Latency measurements

### Backend

- [ ] Spring Boot REST API
- [ ] WebSocket market data
- [ ] PostgreSQL persistence
- [ ] Docker support

### Documentation

- [ ] Architecture diagrams
- [ ] Performance report
- [ ] Design decisions
- [ ] Benchmark results

---

## Learning Objectives

This project focuses on understanding:

- Matching engine algorithms
- Price-time priority
- Exchange order books
- Efficient in-memory data structures
- Object-oriented design
- Clean architecture
- Unit testing
- Backend engineering
- System design

---

This project is intended for educational purposes and experimentation with exchange matching engine concepts.