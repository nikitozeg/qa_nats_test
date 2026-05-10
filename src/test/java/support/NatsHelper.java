package support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class NatsHelper implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Connection connection;

    private NatsHelper(Connection connection) {
        this.connection = connection;
    }

    public static NatsHelper connect() throws IOException, InterruptedException {
        return new NatsHelper(Nats.connect(TestConfig.NATS_URL));
    }

    public Subscription subscribe(String subject) {
        return connection.subscribe(subject);
    }

    public void publishJson(String subject, String json) {
        connection.publish(subject, json.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Message> awaitMessageForOrder(Subscription sub, String orderId, Duration timeout)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            Duration left = Duration.between(Instant.now(), deadline);
            if (left.isNegative() || left.isZero()) break;
            Message msg = sub.nextMessage(left);
            if (msg == null) return Optional.empty();
            try {
                JsonNode node = MAPPER.readTree(msg.getData());
                JsonNode idNode = node.get("order_id");
                if (idNode != null && orderId.equals(idNode.asText())) {
                    return Optional.of(msg);
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() throws InterruptedException {
        if (connection != null) connection.close();
    }
}
