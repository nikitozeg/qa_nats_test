package contract;

import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;
import support.OrderFactory;
import support.SchemaValidator;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrdersCreateContractTest {

    private static final String SCHEMA = "schemas/orders_create.json";

    @Test
    void validBuyConforms() {
        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA,
                OrderFactory.validBuy(OrderFactory.uniqueSymbol()).toJson());
        assertThat(errors).isEmpty();
    }

    @Test
    void validSellConforms() {
        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA,
                OrderFactory.validSell(OrderFactory.uniqueSymbol()).toJson());
        assertThat(errors).isEmpty();
    }

    @Test
    void schemaRejectsMissingField() {
        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA,
                OrderFactory.validBuy("AAPL").remove("price").toJson());
        assertThat(errors).isNotEmpty();
    }

    @Test
    void schemaRejectsNegativeQty() {
        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA,
                OrderFactory.validBuy("AAPL").quantity(-1).toJson());
        assertThat(errors).isNotEmpty();
    }

    @Test
    void schemaRejectsUnknownSide() {
        Set<ValidationMessage> errors = SchemaValidator.validate(SCHEMA,
                OrderFactory.validBuy("AAPL").side("HOLD").toJson());
        assertThat(errors).isNotEmpty();
    }
}
