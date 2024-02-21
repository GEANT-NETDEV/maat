package org.geant.maat.ResourceCrud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;


class UpdateTest extends ResourceTest {

    @TestFactory
    @DisplayName("Updating certain properties should not be allowed")
    Stream<DynamicTest> idOrHref() {
        return Stream.of("id", "href", "@schemaLocation", "@type").map(p -> dynamicTest(p, () -> assertTrue(
                resourceService.updateResource(resource123.getId(), oneFieldJson(p, "12345"), false).isLeft()
        )));
    }

    @DisplayName("Update according to schema should be allowed")
    @Test
    void okUpdate() {
        var result = resourceService.updateResource(resource123.getId(), oneFieldJson("name", "abc"), false).get();

        assertEquals("abc", result.get("name").asText());
    }

    @DisplayName("Updating properties with incorrect type should not be allowed")
    @Test
    void wrongTypeUpdate() {
        var result = resourceService.updateResource(resource123.getId(),
                                                    oneFieldJson("resourceRelationship", "not_array"),
                                                    false);
        assertTrue(result.isLeft());
    }

    @DisplayName("updating with property: null should remove it")
    @Test
    void nullUpdate() {
        var result = resourceService.updateResource(resource123.getId(), oneFieldJson("name", null), false).get();

        assertFalse(result.has("name"));
    }


    private JsonNode oneFieldJson(String name, String value) {
        return new ObjectMapper().createObjectNode().put(name, value);
    }
}
