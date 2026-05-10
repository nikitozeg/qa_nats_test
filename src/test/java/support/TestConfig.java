package support;

import java.time.Duration;

public final class TestConfig {

    public static final String NATS_URL = "nats://localhost:4222";
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/trading";
    public static final String DB_USER = "testuser";
    public static final String DB_PASSWORD = "testpass";

    public static final String SUBJ_ORDERS_CREATE = "orders.create";
    public static final String SUBJ_ORDERS_CONFIRMED = "orders.confirmed";
    public static final String SUBJ_ORDERS_REJECTED = "orders.rejected";
    public static final String SUBJ_TRADES_EXECUTED = "trades.executed";

    public static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration NEGATIVE_EVENT_WAIT = Duration.ofMillis(1500);
    public static final Duration DB_POLL_TIMEOUT = Duration.ofSeconds(5);

    private TestConfig() {}
}
