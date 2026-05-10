package e2e;

import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import support.NatsHelper;
import support.OrderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;

class HappyPathTest {

    private static NatsHelper nats;

    @BeforeAll
    static void setUp() throws Exception {
        nats = NatsHelper.connect();
    }

    @AfterAll
    static void tearDown() throws Exception {
        nats.close();
    }

    @Test
    void buyOrderIsConfirmed() throws Exception {
        Subscription sub = nats.subscribe(SUBJ_ORDERS_CONFIRMED);

        OrderFactory order = OrderFactory.validBuy("AAPL");
        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Message msg = sub.nextMessage(EVENT_TIMEOUT);
        assertThat(msg).as("orders.confirmed must arrive within %s", EVENT_TIMEOUT).isNotNull();

        String body = new String(msg.getData());
        assertThat(body).contains("\"status\": \"CONFIRMED\"");
        assertThat(body).contains(order.orderId());
    }
}
