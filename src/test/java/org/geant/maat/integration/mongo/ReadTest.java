package org.geant.maat.integration.mongo;

import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@DisplayName("When 3 resources are already added")
@Testcontainers
class ReadTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    private ResourceService resourceService;

    // TODO move toJson to separated class and import here and in Create class
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
        IntStream.rangeClosed(1, 3).forEach(id -> resourceService.createResource(toJson(createResourceString(id)), false));
    }

    private String createResourceString(int id) {
        return String.format("{\"name\": \"%s\", \"value\": %d}", "name_" + id, id);
    }

    @Test
    @DisplayName("It should be able to read all of them")
    void readAll() {
        var resources =  resourceService.getResources(Collections.emptyList(), Collections.emptyMap(), "");
        assertEquals(3,resources.size());
    }


    @TestFactory
    @DisplayName("It should be able to read each by id")
    Stream<DynamicTest> readById() {
        return resourceService.getResources(Collections.emptyList(), Collections.emptyMap(), "").stream()
                .map(jsonNode -> jsonNode.get("id").asText())
                .map(id -> dynamicTest(String.format("for id '%s", id),
                                       () -> assertTrue(resourceService.getResource(id).isPresent())));
    }
}
