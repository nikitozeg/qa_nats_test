package support;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class NatsHelper implements AutoCloseable {

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

    @Override
    public void close() throws InterruptedException {
        if (connection != null) connection.close();
    }
}
