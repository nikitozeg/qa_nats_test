package support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public final class OrderFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectNode node;

    private OrderFactory(ObjectNode node) {
        this.node = node;
    }

    public static OrderFactory validBuy(String symbol) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("order_id", UUID.randomUUID().toString());
        n.put("symbol", symbol);
        n.put("side", "BUY");
        n.put("quantity", 10);
        n.put("price", 150.0);
        return new OrderFactory(n);
    }

    public String orderId() {
        return node.get("order_id").asText();
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
