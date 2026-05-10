package support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class DbHelper implements AutoCloseable {

    private final Connection connection;

    private DbHelper(Connection connection) {
        this.connection = connection;
    }

    public static DbHelper connect() throws SQLException {
        Connection c = DriverManager.getConnection(
                TestConfig.DB_URL, TestConfig.DB_USER, TestConfig.DB_PASSWORD);
        c.setAutoCommit(true);
        return new DbHelper(c);
    }

    public void truncateAll() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE orders, positions");
        }
    }

    public Optional<OrderRow> findOrder(String orderId) throws SQLException {
        String sql = "SELECT order_id, symbol, side, quantity, status FROM orders WHERE order_id = ?::uuid";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new OrderRow(
                        rs.getString("order_id"),
                        rs.getString("symbol"),
                        rs.getString("side"),
                        rs.getInt("quantity"),
                        rs.getString("status")
                ));
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public record OrderRow(String orderId, String symbol, String side, int quantity, String status) {}
}
