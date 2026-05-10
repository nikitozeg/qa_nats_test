package support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SchemaValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final ConcurrentHashMap<String, JsonSchema> CACHE = new ConcurrentHashMap<>();

    private SchemaValidator() {}

    public static Set<ValidationMessage> validate(String schemaResource, String json) {
        JsonSchema schema = CACHE.computeIfAbsent(schemaResource, SchemaValidator::loadSchema);
        try {
            JsonNode node = MAPPER.readTree(json);
            return schema.validate(node);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse JSON for schema " + schemaResource, e);
        }
    }

    public static Set<ValidationMessage> validate(String schemaResource, byte[] payload) {
        return validate(schemaResource, new String(payload, StandardCharsets.UTF_8));
    }

    private static JsonSchema loadSchema(String resource) {
        try (InputStream in = SchemaValidator.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Schema not found: " + resource);
            }
            return FACTORY.getSchema(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema " + resource, e);
        }
    }
}
