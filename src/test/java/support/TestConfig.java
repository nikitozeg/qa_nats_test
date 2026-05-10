package support;

import java.time.Duration;

public final class TestConfig {

    public static final String NATS_URL = "nats://localhost:4222";

    public static final String SUBJ_ORDERS_CREATE = "orders.create";
    public static final String SUBJ_ORDERS_CONFIRMED = "orders.confirmed";

    public static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);

    private TestConfig() {}
}
