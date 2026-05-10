package e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import support.BaseIntegrationTest;
import support.OrderFactory;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static support.TestConfig.EVENT_TIMEOUT;
import static support.TestConfig.NEGATIVE_EVENT_WAIT;
import static support.TestConfig.SUBJ_ORDERS_CONFIRMED;
import static support.TestConfig.SUBJ_ORDERS_CREATE;
import static support.TestConfig.SUBJ_ORDERS_REJECTED;
import static support.TestConfig.SUBJ_TRADES_EXECUTED;

class NegativeValidationTest extends BaseIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    static Stream<Arguments> invalidOrders() {
        String s = OrderFactory.uniqueSymbol();
        return Stream.of(
                arguments("missing symbol",    OrderFactory.validBuy(s).remove("symbol")),
                arguments("missing side",      OrderFactory.validBuy(s).remove("side")),
                arguments("missing quantity",  OrderFactory.validBuy(s).remove("quantity")),
                arguments("missing price",     OrderFactory.validBuy(s).remove("price")),
                arguments("zero quantity",     OrderFactory.validBuy(s).quantity(0)),
                arguments("negative quantity", OrderFactory.validBuy(s).quantity(-5)),
                arguments("zero price",        OrderFactory.validBuy(s).price(0.0)),
                arguments("negative price",    OrderFactory.validBuy(s).price(-10.0)),
                arguments("unknown side",      OrderFactory.validBuy(s).side("HOLD")),
                arguments("lowercase side",    OrderFactory.validBuy(s).side("buy"))
        );
    }

    @ParameterizedTest(name = "[{index}] rejects: {0}")
    @MethodSource("invalidOrders")
    void invalidOrderIsRejected(String label, OrderFactory order) throws Exception {
        String orderId = order.orderId();
        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Optional<Message> rejected = nats.awaitMessageForOrder(rejectedSub, orderId, EVENT_TIMEOUT);
        assertThat(rejected).as("orders.rejected for: %s", label).isPresent();
        JsonNode body = MAPPER.readTree(rejected.get().getData());
        assertThat(body.get("status").asText()).isEqualTo("REJECTED");

        assertThat(nats.awaitMessageForOrder(confirmedSub, orderId, NEGATIVE_EVENT_WAIT)).isEmpty();
        assertThat(nats.awaitMessageForOrder(executedSub, orderId, NEGATIVE_EVENT_WAIT)).isEmpty();

        assertThat(db.countOrders(orderId)).isZero();
    }

    @Test
    void malformedJsonNeverConfirmsOrTrades() throws Exception {
        nats.publishRaw(SUBJ_ORDERS_CREATE, "{not-json".getBytes());

        Message confirmed = confirmedSub.nextMessage(NEGATIVE_EVENT_WAIT);
        Message executed = executedSub.nextMessage(NEGATIVE_EVENT_WAIT);

        assertThat(confirmed).isNull();
        assertThat(executed).isNull();
    }

    @Test
    void missingOrderIdNeverProducesATrade() throws Exception {
        OrderFactory order = OrderFactory.validBuy(OrderFactory.uniqueSymbol()).remove("order_id");

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Message executed = executedSub.nextMessage(NEGATIVE_EVENT_WAIT);
        assertThat(executed).isNull();
    }

    @Test
    void invalidUuidIsNotTraded() throws Exception {
        OrderFactory order = OrderFactory.validBuy(OrderFactory.uniqueSymbol()).orderId("not-a-uuid");

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Message executed = executedSub.nextMessage(NEGATIVE_EVENT_WAIT);
        assertThat(executed).isNull();
    }

    @Test
    void emptySymbolIsNotRejectedCurrently() throws Exception {
        OrderFactory order = OrderFactory.validBuy("AAPL").symbol("");
        String orderId = order.orderId();

        nats.publishJson(SUBJ_ORDERS_CREATE, order.toJson());

        Optional<Message> rejected = nats.awaitMessageForOrder(rejectedSub, orderId, EVENT_TIMEOUT);
        assertThat(rejected).isEmpty();
    }
}
