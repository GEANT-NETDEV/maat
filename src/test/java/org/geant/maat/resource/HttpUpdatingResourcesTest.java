package org.geant.maat.resource;

import org.geant.maat.utils.ResourceHttpClient;
import org.geant.maat.utils.ResourceReader;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
class HttpUpdatingResourcesTest extends HttpTest {
    private final ResourceHttpClient client = new ResourceHttpClient();
    private String resourceId;

    @BeforeEach
    void init() {
        var response = client.sendPost(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        resourceId = JsonPath.parse(response.body()).read("$.id");
    }

    @Test
    @DisplayName("When updating resource should get 200 and whole object with changes")
    void deleteById() {
        var response = client.sendPatchAndParse(resourceUrl() + resourceId, "{\"category\": \"xyz\"}");
        assertEquals("xyz", response.get("category").asText());
    }

    @Test
    @DisplayName("When trying to update forbidden properties should get 409")
    void deleteByWrongId() {
        var response = client.sendPatch(resourceUrl() + resourceId, "{\"id\": \"1234\"}");
        assertEquals(HttpStatus.CONFLICT.value(), response.statusCode());
    }
}
