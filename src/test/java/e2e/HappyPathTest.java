package e2e;

import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.BaseIntegrationTest;
import support.DbHelper;
import support.OrderFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static support.TestConfig.DB_POLL_TIMEOUT;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;
import static support.TestConfig.SUBJ_TRADES_EXECUTED;

class HappyPathTest extends BaseIntegrationTest {

    private Subscription confirmedSub;
    private Subscription executedSub;

    @BeforeEach
    void subscribe() {
        confirmedSub = nats.subscribe(SUBJ_ORDERS_CONFIRMED);
        executedSub = nats.subscribe(SUBJ_TRADES_EXECUTED);
    }

    @AfterEach
    void unsubscribe() throws Exception {
        confirmedSub.unsubscribe();
        executedSub.unsubscribe();
    }

    @Test
    void buyOrderFullFlow() throws Exception {
        String symbol = OrderFactory.uniqueSymbol();
        OrderFactory order = OrderFactory.validBuy(symbol).quantity(50);

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Optional<Message> confirmed = nats.awaitMessageForOrder(confirmedSub, order.orderId(), EVENT_TIMEOUT);
        assertThat(confirmed).as("orders.confirmed").isPresent();

        Optional<Message> executed = nats.awaitMessageForOrder(executedSub, order.orderId(), EVENT_TIMEOUT);
        assertThat(executed).as("trades.executed").isPresent();

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() -> {
            Optional<DbHelper.OrderRow> row = db.findOrder(order.orderId());
            assertThat(row).isPresent();
            assertThat(row.get().symbol()).isEqualTo(symbol);
            assertThat(row.get().side()).isEqualTo("BUY");
            assertThat(row.get().quantity()).isEqualTo(50);
        });

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() ->
                assertThat(db.findPositionQty(symbol)).contains(50));
    }

    @Test
    void sellOrderGoesNegativeWithoutPriorBuy() throws Exception {
        String symbol = OrderFactory.uniqueSymbol();
        OrderFactory order = OrderFactory.validSell(symbol).quantity(7);

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Optional<Message> confirmed = nats.awaitMessageForOrder(confirmedSub, order.orderId(), EVENT_TIMEOUT);
        assertThat(confirmed).isPresent();

        Optional<Message> executed = nats.awaitMessageForOrder(executedSub, order.orderId(), EVENT_TIMEOUT);
        assertThat(executed).isPresent();

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() ->
                assertThat(db.findPositionQty(symbol)).contains(-7));
    }
}
