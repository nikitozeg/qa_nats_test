package e2e;

import io.nats.client.Message;
import io.nats.client.Subscription;
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

class HappyPathTest extends BaseIntegrationTest {

    @Test
    void buyOrderIsConfirmedAndPersisted() throws Exception {
        Subscription sub = nats.subscribe(SUBJ_ORDERS_CONFIRMED);

        OrderFactory order = OrderFactory.validBuy("AAPL");
        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Message msg = sub.nextMessage(EVENT_TIMEOUT);
        assertThat(msg).as("orders.confirmed must arrive within %s", EVENT_TIMEOUT).isNotNull();

        String body = new String(msg.getData());
        assertThat(body).contains("\"status\": \"CONFIRMED\"");
        assertThat(body).contains(order.orderId());

        await().atMost(DB_POLL_TIMEOUT).untilAsserted(() -> {
            Optional<DbHelper.OrderRow> row = db.findOrder(order.orderId());
            assertThat(row).isPresent();
            DbHelper.OrderRow r = row.get();
            assertThat(r.symbol()).isEqualTo("AAPL");
            assertThat(r.side()).isEqualTo("BUY");
            assertThat(r.quantity()).isEqualTo(10);
        });
    }
}
