package e2e;

import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.BaseIntegrationTest;
import support.OrderFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static support.TestConfig.DB_POLL_TIMEOUT;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CREATE;
import static support.TestConfig.SUBJ_TRADES_EXECUTED;

class PositionsTest extends BaseIntegrationTest {

    private Subscription executedSub;

    @BeforeEach
    void subscribe() {
        executedSub = nats.subscribe(SUBJ_TRADES_EXECUTED);
    }

    @AfterEach
    void unsubscribe() throws Exception {
        executedSub.unsubscribe();
    }

    @Test
    void twoBuysAccumulate() throws Exception {
        String symbol = OrderFactory.uniqueSymbol();
        publishAndWait(OrderFactory.validBuy(symbol).quantity(10));
        publishAndWait(OrderFactory.validBuy(symbol).quantity(30));

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() ->
                assertThat(db.findPositionQty(symbol)).contains(40));
    }

    @Test
    void buyThenSell() throws Exception {
        String symbol = OrderFactory.uniqueSymbol();
        publishAndWait(OrderFactory.validBuy(symbol).quantity(100));
        publishAndWait(OrderFactory.validSell(symbol).quantity(25));

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() ->
                assertThat(db.findPositionQty(symbol)).contains(75));
    }

    @Test
    void positionsAreIsolatedPerSymbol() throws Exception {
        String s1 = OrderFactory.uniqueSymbol();
        String s2 = OrderFactory.uniqueSymbol();

        publishAndWait(OrderFactory.validBuy(s1).quantity(11));
        publishAndWait(OrderFactory.validBuy(s2).quantity(22));
        publishAndWait(OrderFactory.validSell(s1).quantity(3));

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() -> {
            assertThat(db.findPositionQty(s1)).contains(8);
            assertThat(db.findPositionQty(s2)).contains(22);
        });
    }

    private void publishAndWait(OrderFactory order) throws Exception {
        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());
        Optional<Message> executed = nats.awaitMessageForOrder(executedSub, order.orderId(), EVENT_TIMEOUT);
        assertThat(executed).isPresent();
    }
}
