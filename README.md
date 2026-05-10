
## Run

```bash
docker compose up -d
mvn test
```

NATS at `localhost:4222`, Postgres at `localhost:5432` (`testuser`/`testpass`/`trading`). All defaults are baked into `support.TestConfig`, no env setup needed.

## What's covered

E2E:
- happy path: BUY and SELL flow all the way through both services, position lands in DB
- negative validation: missing fields, zero/negative quantity and price, unknown side, lowercase side, malformed JSON, invalid UUID. All rejected, nothing reaches the trade service or DB.
- positions: BUY+BUY accumulates, BUY-SELL nets out, two symbols don't interfere
- idempotency: same `order_id` twice doesn't double the position, only one row remains

Contract: JSON Schema for each NATS subject. Producer side (`orders.create` payloads we send) and consumer side (`orders.confirmed`, `orders.rejected`, `trades.executed` events the services emit).

## Layout

```
src/test/java/support/    helpers (NatsHelper, DbHelper, OrderFactory, SchemaValidator, BaseIntegrationTest)
src/test/java/e2e/        e2e tests
src/test/java/contract/   contract tests
src/test/resources/       json schemas + logback config
```

## Approach

Single fork, no parallel , these are integration tests sharing real NATS subjects and a real DB, deterministic ordering matters more than speed.
 Subscriptions are filtered by `order_id` (`NatsHelper.awaitMessageForOrder`) so events from one test don't bleed into another. Every test uses a fresh UUID and a unique short symbol on top of `truncate orders, positions` before each test.
DB writes lag the NATS event slightly, so DB assertions poll via Awaitility within a 5s window.

Contract tests use networknt json-schema-validator with default config — format keywords (`uuid`, `date-time`) are annotations only, types/required/enums/ranges are still asserted.

## Spec gaps and how I read them

- **status field after confirmation.** Spec says the row is saved with `CREATED`, then `orders.confirmed` is published. Doesn't say if status is later updated to `CONFIRMED`. Test accepts either.
- **rejected orders in the DB.** Reading the spec as "validate, save, publish", a rejected order shouldn't reach the DB. Tests assert `countOrders == 0`.
- **SELL with no prior buy.** `positions.quantity` is signed `INTEGER` with no CHECK. The service drives the position negative. Test asserts that.
- **empty `symbol`.** Spec says `string (e.g. AAPL)` without an explicit non-empty constraint. The service doesn't reject it. `emptySymbolIsNotRejectedCurrently` pins the current behaviour — if it ever changes, the test fails and the team can decide if that's a fix or a regression.
- **duplicate `order_id`.** Not specified. The current image rejects the duplicate with `orders.rejected` and no second trade. Test asserts the invariants (no double-apply, exactly one row) rather than the specific outcome, so the suite stays robust if the implementation switches to "swallow silently".

## Requirements

Java 21+ (uses records), Maven 3.9+, Docker + Docker Compose. On macOS: `brew install maven`.
