package contract;

import com.networknt.schema.ValidationMessage;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.BaseIntegrationTest;
import support.OrderFactory;
import support.SchemaValidator;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;

class OrdersConfirmedContractTest extends BaseIntegrationTest {

    private static final String SCHEMA = "schemas/orders_confirmed.json";

    private Subscription sub;

    @BeforeEach
    void subscribe() {
        sub = nats.subscribe(SUBJ_ORDERS_CONFIRMED);
    }

    @AfterEach
    void unsubscribe() throws Exception {
        sub.unsubscribe();
    }

    @Test
    void confirmedEventMatchesSchema() throws Exception {
        OrderFactory order = OrderFactory.validBuy(OrderFactory.uniqueSymbol());
        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Optional<Message> msg = nats.awaitMessageForOrder(sub, order.orderId(), EVENT_TIMEOUT);
        assertThat(msg).isPresent();

        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA, msg.get().getData());
        assertThat(errors).isEmpty();
    }
}
