package org.geant.maat.resource;

import org.geant.maat.utils.ResourceHttpClient;
import org.geant.maat.utils.ResourceReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
class HttpCreatingResourcesTest extends HttpTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceHttpClient client = new ResourceHttpClient();
    private final String example_resource = ResourceReader.getLogicalResource();

    @Test
    @DisplayName("When POSTing resource then it should be created")
    public void create() {
        var response = client.sendPost(resourceUrlNoSlash(), example_resource);
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("When trying to create resource that does not match the schema then should get 404")
    void creatingIncorrect() throws IOException {
        ObjectNode on = (ObjectNode) mapper.readTree(example_resource);
        on.put("name", 1234);

        var response = client.sendPost(resourceUrlNoSlash(), on.toString());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    @Test
    @DisplayName("GETting href from resource creation response should return the same resource")
    public void getByHref() {
        var response = client.sendPost(resourceUrlNoSlash(), example_resource);
        String href = JsonPath.parse(response.body()).read("$.href");
        String id = JsonPath.parse(response.body()).read("$.id");
        response = client.sendGet(href);
        String new_id = JsonPath.parse(response.body()).read("$.id");

        assertEquals(response.statusCode(), HttpStatus.OK.value());
        assertEquals(id, new_id);
    }

    @Test
    @DisplayName("When trying to get resource by id that does not exist then should get 404")
    void getNonExistingId() {
        var response = client.sendGet(resourceUrl() + "badId");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
    }
}