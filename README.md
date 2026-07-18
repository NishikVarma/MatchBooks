# Order Book Matching Engine

A Java implementation of an in-memory limit order book matching engine that follows **price-time priority** for matching BUY and SELL limit orders.

The goal of this project is to understand the core algorithms and data structures behind modern electronic exchanges by implementing the matching engine from scratch without external dependencies.

## Features

* Limit order support
* Price-time priority matching
* FIFO ordering within each price level
* Partial order fills
* Complete order fills
* Automatic removal of fully executed orders
* Trade generation for every successful match
* In-memory implementation using Java collections

## Matching Rules

### Price Priority

* BUY orders match the **lowest available SELL price**.
* SELL orders match the **highest available BUY price**.

Example:

```
SELL 50 @ 100
SELL 30 @ 101

BUY 60 @ 101
```

Execution:

```
BUY 60
├── matches SELL 50 @ 100
└── matches SELL 10 @ 101
```

Remaining order book:

```
SELL 20 @ 101
```

---

### Time Priority (FIFO)

Orders at the same price are matched in the order they were received.

Example:

```
SELL 20 @ 100
SELL 30 @ 100
SELL 40 @ 100

BUY 35 @ 100
```

Execution:

```
SELL 20 -> fully executed
SELL 30 -> partially executed (15 remaining)
SELL 40 -> untouched
```

Remaining book:

```
SELL 15 @ 100
SELL 40 @ 100
```

## Data Structures

```text
BUY Book
TreeMap<Double, Queue<Order>>
        ↓
Highest Price
        ↓
FIFO Queue

SELL Book
TreeMap<Double, Queue<Order>>
        ↓
Lowest Price
        ↓
FIFO Queue
```

* `TreeMap` maintains price levels in sorted order.
* BUY orders use a reverse-order `TreeMap`.
* SELL orders use a natural-order `TreeMap`.
* Each price level stores a FIFO `Queue<Order>` to preserve time priority.

## Project Structure

```
src/
├── Main.java
├── Order.java
├── OrderBook.java
├── Side.java
└── Trade.java
```

### Order

Represents a limit order.

Fields:

* Side (BUY / SELL)
* Quantity
* Price

### Trade

Represents a completed trade.

Fields:

* Price
* Quantity

### OrderBook

Responsible for:

* Maintaining BUY and SELL books
* Matching incoming orders
* Updating quantities
* Removing completed orders
* Generating trades

## Complexity

| Operation                |    Complexity |
| ------------------------ | ------------: |
| Add Order                |      O(log n) |
| Best Bid / Ask Lookup    |          O(1) |
| Match Against Best Order | O(1) per fill |
| Remove Empty Price Level |      O(log n) |

Where *n* is the number of price levels in the order book.

## Example

```java
OrderBook orderBook = new OrderBook();

orderBook.processOrder(new Order(Side.SELL, 20, 100));
orderBook.processOrder(new Order(Side.SELL, 30, 100));
orderBook.processOrder(new Order(Side.BUY, 35, 100));

orderBook.printOrderBook();
```

Output:

```
TRADE -> Quantity: 20, Price: 100.0
TRADE -> Quantity: 15, Price: 100.0

Side | Quantity | Price
SELL | 15 | 100.0
```

## Current Limitations

This project currently supports:

* Limit orders only
* Single-threaded execution
* In-memory order book

The following features are planned:

* Order IDs
* Order cancellation
* Order modification
* Market orders
* JUnit test suite
* Performance benchmarking
* Concurrent matching engine
* REST API
* Persistence
* WebSocket market data streaming

## Learning Goals

This project focuses on understanding:

* Price-time priority matching
* Efficient data structures for order books
* Queue-based scheduling (FIFO)
* Partial fill handling
* Trade generation
* Matching engine design

## License

This project is intended for educational purposes and experimentation with exchange matching engine concepts.
