package org.geant.maat.integration.mongo;

import org.geant.maat.resource.Resource;
import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Nested
@DisplayName("it should be able to filter fields")
@Testcontainers
class FilteringTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    private ResourceService service;
    private Resource resource;

    @BeforeEach
    void init() {
        ObjectNode on = new ObjectMapper().createObjectNode();
        on.put("name", "name");
        on.put("value", 123);
        service = new ResourceConfiguration().resourceServiceWithTestMongo();
        resource = new Resource(service.createResource(on, false).get());
    }

    @Test
    @DisplayName("When getting resource by id and empty list of fields is passed then all fields should be returned")
    void emptyFieldList() {
        var json = getJsonWithFields(Collections.emptyList());

        assertHasAllFields(json);
    }

    private void assertHasAllFields(JsonNode json) {
        Arrays.asList("id", "href", "name", "value").forEach(prop -> Assertions.assertTrue(json.has(prop), "Property not found: " + prop));
    }

    @TestFactory
    @DisplayName("When getting all resources and empty list of fields and filtering is passed then all resources and all fields should be returned")
    Stream<DynamicTest> emptyFieldsAndFiltering() {
        var response = service.getResources(Collections.emptyList(), Collections.emptyMap());
        return response.stream()
                .map(json -> dynamicTest(
                        json.toString(),
                        () -> assertHasAllFields(json)));
    }

    private JsonNode getJsonWithFields(Collection<String> names) {
        var filtered = service.getResource(resource.getId(), names);
        Assertions.assertTrue(filtered.isRight());
        return filtered.get();
    }

    @Test
    @DisplayName("When list of props to is passed then only these fields should be returned")
    void validPropsPresent() {
        var json = getJsonWithFields(Collections.singletonList("name"));

        assertEquals(1, countElements(json.fieldNames()));
        Assertions.assertTrue(json.has("name"));
    }

    private long countElements(Iterator<String> iterator) {
        Iterable<String> iterable = () -> iterator;
        Stream<String> targetStream = StreamSupport.stream(iterable.spliterator(), false);
        return targetStream.count();
    }

    @Test
    @DisplayName("When prop in fields variable is missing in resource then it should not be present in result json")
    void invalidPropsPresent() {
        var json = getJsonWithFields(Arrays.asList("name", "missing"));

        assertEquals(1, countElements(json.fieldNames()));
        assertTrue(json.has("name"));
    }

    @Test
    @DisplayName("When passing filtering then only resource matching criteria should be returned")
    void filtering() {
        Map<String, String> filtering = Map.of("name", "123");
        ObjectNode on = new ObjectMapper().createObjectNode();
        on.put("name", "123");
        service.createResource(on, false);

        var all = service.getResources(Collections.emptyList(), Map.of());
        var filtered = service.getResources(Collections.emptyList(), filtering);

        assertEquals(2, all.size());
        assertEquals(1, filtered.size());
    }
}