package org.geant.maat.resource;

import org.geant.maat.utils.ResourceHttpClient;
import org.geant.maat.utils.ResourceReader;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpDeletingResourcesTest extends HttpTest {
    private final ResourceHttpClient client = new ResourceHttpClient();
    private String resourceId;

    @BeforeAll
    void init() {
        var response = client.sendPost(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        resourceId = JsonPath.parse(response.body()).read("$.id");
    }

    @Test
    @DisplayName("When deleting existing resource by id should get 204")
    void deleteById() {
        var response = client.sendDelete(resourceUrl() + resourceId);
        assertEquals(HttpStatus.NO_CONTENT.value(), response.statusCode());
    }

    @Test
    @DisplayName("When deleting nonexistent resource by id should get 404")
    void deleteByWrongId() {
        var response = client.sendDelete(resourceUrl() + "1234");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
    }
}
