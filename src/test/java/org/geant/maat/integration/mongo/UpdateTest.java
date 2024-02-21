package org.geant.maat.integration.mongo;

import org.geant.maat.resource.Resource;
import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import org.geant.maat.utils.ResourceReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;


@Testcontainers
class UpdateTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    private ResourceService resourceService;
    private Resource createdResource;

    @BeforeEach
    void init() {
        resourceService = new ResourceConfiguration().resourceServiceWithTestMongo();
        createdResource = Resource.from(
                resourceService.createResource(ResourceReader.getDefaultResource(), false).get().toString());
    }

    @TestFactory
    @DisplayName("Updating certain properties should not be allowed")
    Stream<DynamicTest> idOrHref() {
        return Stream.of("id", "href", "@schemaLocation", "@type").map(p -> dynamicTest(p, () -> assertTrue(
                resourceService.updateResource(createdResource.getId(), oneFieldJson(p, "12345"), false).isLeft()
        )));
    }

    @DisplayName("Update according to schema should be allowed")
    @Test
    void okUpdate() {
        var result = resourceService.updateResource(createdResource.getId(), oneFieldJson("name", "abc"), false);
        assertTrue(result.isRight());
        assertEquals("abc", result.get().get("name").asText());
    }

    @DisplayName("Updating properties with incorrect type should not be allowed")
    @Test
    void wrongTypeUpdate() {
        var result = resourceService.updateResource(createdResource.getId(),
                                                    oneFieldJson("resourceRelationship", "not_array"),
                                                    false);
        assertTrue(result.isLeft());
    }

    @DisplayName("updating with property: null should remove it")
    @Test
    void nullUpdate() {
        var result = resourceService.updateResource(createdResource.getId(), oneFieldJson("name", null), false);

        assertTrue(result.isRight());
        assertFalse(result.get().has("name"));
    }


    private JsonNode oneFieldJson(String name, String value) {
        var on = new ObjectMapper().createObjectNode();
        return on.put(name, value);
    }
}
