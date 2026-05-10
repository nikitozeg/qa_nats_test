package e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import support.NatsHelper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;

class HappyPathTest {

    private static NatsHelper nats;
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        String orderId = UUID.randomUUID().toString();
        ObjectNode order = MAPPER.createObjectNode()
                .put("order_id", orderId)
                .put("symbol", "AAPL")
                .put("side", "BUY")
                .put("quantity", 10)
                .put("price", 150.0);

        nats.publishJson(SUBJ_ORDERS_CREATE, MAPPER.writeValueAsString(order));

        Message msg = sub.nextMessage(EVENT_TIMEOUT);
        assertThat(msg).as("orders.confirmed must arrive within %s", EVENT_TIMEOUT).isNotNull();

        String body = new String(msg.getData());
        assertThat(body).contains("\"status\": \"CONFIRMED\"");
        assertThat(body).contains(orderId);
    }
}
