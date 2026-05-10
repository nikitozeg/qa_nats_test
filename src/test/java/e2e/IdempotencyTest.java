package e2e;

import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.BaseIntegrationTest;
import support.OrderFactory;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static support.TestConfig.DB_POLL_TIMEOUT;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;
import static support.TestConfig.SUBJ_ORDERS_REJECTED;
import static support.TestConfig.SUBJ_TRADES_EXECUTED;

class IdempotencyTest extends BaseIntegrationTest {

    private Subscription confirmedSub;
    private Subscription rejectedSub;
    private Subscription executedSub;

    @BeforeEach
    void subscribe() {
        confirmedSub = nats.subscribe(SUBJ_ORDERS_CONFIRMED);
        rejectedSub = nats.subscribe(SUBJ_ORDERS_REJECTED);
        executedSub = nats.subscribe(SUBJ_TRADES_EXECUTED);
    }

    @AfterEach
    void unsubscribe() throws Exception {
        confirmedSub.unsubscribe();
        rejectedSub.unsubscribe();
        executedSub.unsubscribe();
    }

    @Test
    void duplicateOrderIdIsNotDoubleApplied() throws Exception {
        String symbol = OrderFactory.uniqueSymbol();
        OrderFactory order = OrderFactory.validBuy(symbol).quantity(40);
        String orderId = order.orderId();

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());
        assertThat(nats.awaitMessageForOrder(confirmedSub, orderId, EVENT_TIMEOUT)).isPresent();
        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() ->
                assertThat(db.findOrder(orderId)).isPresent());

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());
        Thread.sleep(Duration.ofSeconds(2).toMillis());

        assertThat(db.countOrders(orderId)).isEqualTo(1);
        assertThat(db.findPositionQty(symbol)).contains(40);

        List<Message> rejects = nats.drainMessagesForOrder(rejectedSub, orderId, Duration.ofMillis(500));
        List<Message> trades = nats.drainMessagesForOrder(executedSub, orderId, Duration.ofMillis(500));
        assertThat(rejects).as("duplicate must not produce more than one rejection").hasSizeLessThanOrEqualTo(1);
        assertThat(trades).as("duplicate must not produce a second trade").hasSize(1);
    }
}
