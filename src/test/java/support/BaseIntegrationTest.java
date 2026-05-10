package support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseIntegrationTest {

    protected static NatsHelper nats;
    protected static DbHelper db;

    @BeforeAll
    static void openConnections() throws Exception {
        nats = NatsHelper.connect();
        db = DbHelper.connect();
    }

    @AfterAll
    static void closeConnections() throws Exception {
        if (nats != null) nats.close();
        if (db != null) db.close();
    }

    @BeforeEach
    void resetState() throws Exception {
        db.truncateAll();
    }
}
