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

    public static OrderFactory validSell(String symbol) {
        return validBuy(symbol).side("SELL");
    }

    public static String uniqueSymbol() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    public OrderFactory orderId(String value) { node.put("order_id", value); return this; }
    public OrderFactory symbol(String value)  { node.put("symbol", value);   return this; }
    public OrderFactory side(String value)    { node.put("side", value);     return this; }
    public OrderFactory quantity(int value)   { node.put("quantity", value); return this; }
    public OrderFactory price(double value)   { node.put("price", value);    return this; }
    public OrderFactory remove(String field)  { node.remove(field);          return this; }

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
