package org.geant.maat.integration.mongo;

import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER;
import static org.junit.jupiter.api.Assertions.assertTrue;


// TODO Parametrize database in test for resource service to avoid awful copy paste
// TODO Also do not run tests with mongo always - create some profile
@Testcontainers
@DisplayName("When creating resource from json")
@TestPropertySource(properties = {"notification.sendNotificationToListeners=true"})
class CreateTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    private final String stringResource = "{\"name\": \"abc123\", \"value\": 123}";
    private ResourceService resourceService;
    private ObjectNode json;

    private JsonNode toJson(String string) {
        try {
            return new ObjectMapper().readTree(string);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot parse json from: '" + string + "'");
        }
    }

    @BeforeEach
    void init() {
        resourceService = new ResourceConfiguration().resourceServiceWithTestMongo();
        json = (ObjectNode) toJson(stringResource);
    }

    @Test
    @DisplayName("mandatory properties - 'id' and 'href' should be added")
    void shouldContainMandatory() {
        var resource = resourceService.createResource(json, false).get();

        assertTrue(resource.has("id"));
        assertTrue(resource.has("href"));
    }

    @Nested
    @DisplayName("Schema should be validated")
    class Validation {
        @Test
        @DisplayName("When @type missing should create resource without validation")
        void typeMissing() {
            json.put("@schemaLocation", "ABC");

            var newResource = resourceService.createResource(toJson(stringResource), false);

            assertTrue(newResource.isRight());
        }

        @Test
        @DisplayName("When @schemaLocation missing should create resource without validation")
        void schemaLocationMissing() {
            json.put("@type", "ABC");

            var newResource = resourceService.createResource(toJson(stringResource), false);

            assertTrue(newResource.isRight());
        }

        @Nested
        @DisplayName("When both @type and @schemaLocation are present")
        class TypeAndSchemaLocationPresent {
            private static final ObjectMapper mapper = new ObjectMapper();
            private ObjectNode validJson;

            @BeforeEach
            void init() throws IOException {
                mapper.configure(ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

                Function<String, URL> getUrl = (String name) -> getClass().getClassLoader().getResource((name));
                ObjectNode on = (ObjectNode) mapper.readTree(getUrl.apply("resource.json"));
                on.put("@schemaLocation", getUrl.apply("schema.json").toString());
                validJson = on.deepCopy();

                Assertions.assertTrue(validJson.has("@type"));
                Assertions.assertTrue(validJson.has("@schemaLocation"));
            }

            @Test
            @DisplayName("When json describing resource matches the schema resource should be created")
            void goodResource() {
                var resource = resourceService.createResource(validJson, false);

                Assertions.assertTrue(resource.isRight());
            }

            @Test
            @DisplayName("When type doesn't match any definition in schema then should not be created")
            void typeNotMatchingDefinition() {
                validJson.put("@type", "Bad_type");

                var resource = resourceService.createResource(validJson, false);

                Assertions.assertTrue(resource.isLeft());
            }

            @Test
            @DisplayName("When type of property is wrong resource should not be created")
            void wrongType() {
                validJson.put("category", 123);

                var resource = resourceService.createResource(validJson, false);

                Assertions.assertTrue(resource.isLeft());
            }
        }
    }
}