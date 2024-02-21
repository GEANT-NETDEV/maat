package org.geant.maat.ResourceCrud;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class ReadTest extends ResourceTest {
    @Test
    @DisplayName("It should be able to read all of them")
    void readAll() {
        var resources = resourceService.getResources(Collections.emptyList(), Collections.emptyMap());
        assertEquals(2, resources.size());
    }


    @TestFactory
    @DisplayName("It should be able to read each by id")
    Stream<DynamicTest> readById() {
        return resourceService.getResources(Collections.emptyList(), Collections.emptyMap()).stream()
                .map(jsonNode -> jsonNode.get("id").asText())
                .map(id -> dynamicTest(id, () -> assertTrue(resourceService.getResource(id).isPresent())));
    }

    @ParameterizedTest
    @CsvSource({"1, 1, 1", "1, 4, 1", "4, 4, 0"})
    void shouldGetCorrectNumberOfResourcesWithOffsetAndLimit(int offset, int limit, int expectedSize) {
        var resources = resourceService.getResources(List.of(), Map.of(), offset, limit);

        assertEquals(expectedSize, resources.size());
    }

    @Test
    @DisplayName("When getting resource by id and empty list of fields is passed then all fields should be returned")
    void emptyFieldList() {
        var json = resourceService.getResource(resource123.getId(), List.of()).get();

        assertEquals(resource123.toJson(), json);
    }

    @Test
    @DisplayName("When getting resources with empty fields and filtering then all fields and resources should be returned")
    void emptyFieldsAndFiltering() {
        var response = resourceService.getResources(Collections.emptyList(), Collections.emptyMap());

        assertEquals(2, response.size());
        assertTrue(response.containsAll(List.of(resource123.toJson(), resource456.toJson())));
    }

    @Test
    @DisplayName("When list of props to is passed then only these fields should be returned")
    void validPropsPresent() {
        var json = resourceService.getResource(resource123.getId(), List.of("name")).get();

        assertEquals(1, countElements(json.fieldNames()));
        Assertions.assertTrue(json.has("name"));
    }

    @Test
    @DisplayName("When prop in fields variable is missing in resource then it should not be present in result json")
    void invalidPropsPresent() {
        var json = resourceService.getResource(resource123.getId(), List.of("name", "missing")).get();

        assertEquals(1, countElements(json.fieldNames()));
        assertTrue(json.has("name"));
    }

    @Test
    @DisplayName("When passing filtering then only resource matching criteria should be returned")
    void filtering() {
        var all = resourceService.getResources(List.of(), Map.of());
        var filtered = resourceService.getResources(List.of(), Map.of("name", "123"));

        assertEquals(2, all.size());
        assertEquals(1, filtered.size());
    }

    private long countElements(Iterator<String> iterator) {
        Iterable<String> iterable = () -> iterator;
        Stream<String> targetStream = StreamSupport.stream(iterable.spliterator(), false);
        return targetStream.count();
    }
}
