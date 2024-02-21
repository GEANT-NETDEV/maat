package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.geant.maat.utils.ResourceHttpClient;
import org.geant.maat.utils.ResourceReader;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpReadingResourcesTest extends HttpTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceHttpClient client = new ResourceHttpClient();
    private String resourceId;
    private JsonNode createdResource;
    private final ResourceRepository repository;

    @Autowired
    HttpReadingResourcesTest(ResourceRepository repository) {this.repository = repository;}

    @BeforeEach
    void init() throws IOException {
        var response = client.sendPost(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        resourceId = JsonPath.parse(response.body()).read("$.id");
        createdResource = mapper.readTree(response.body());
    }

    @AfterEach
    void clear() {
        repository.clean();
    }

    @Test
    @DisplayName("It should be able to get resource by id")
    void getById() {
        var json = client.sendGetAndParse(resourceUrl() + resourceId);

        String id = JsonPath.parse(json.toString()).read("$.id");
        assertEquals(resourceId, id);
    }

    @Test
    @DisplayName("It should be able to get list of all resources")
    void getAll() {
        var jsons = client.sendGetAndParse(resourceUrlNoSlash());

        for (var json : jsons) {
            assertDoesNotThrow(() -> (new Resource(json)));
        }
    }

    @Test
    @DisplayName("When passing list of fields in request then all other fields should be filtered out")
    void fields() {
        var json = client.sendGetAndParse(resourceUrl()+ resourceId + "?fields=id,href");
        var expectedNames = new HashSet<>(Arrays.asList("id", "href"));

        json.fieldNames().forEachRemaining(name -> {
            assertTrue(expectedNames.contains(name), String.format("'%s' was not filtered out", name));
            expectedNames.remove(name);
        });
        assertEquals(0, expectedNames.size());
    }

    @Test
    @DisplayName("When property in list of fields does not exists in resource then nothing should be returned for it")
    void fieldIncorrect() {
        var json = client.sendGetAndParse(resourceUrl() + resourceId + "?fields=bad_name,id");

        assertTrue(json.has("id"));
        assertFalse(json.has("bad_name"));
    }

    @Test
    @DisplayName("When no fields passed then all fields should be returned")
    void noFields() {
        var json = client.sendGetAndParse(resourceUrl() + resourceId);

        assertEquals(createdResource, json);
    }

    @Test
    @DisplayName("When filtering is present then only resource matching criteria should be returned")
    void filtering() {
        var json = client.sendGetAndParse(resourceUrlNoSlash() + "?category=stp&@type=LogicalResource");
        List<JsonNode> jsons = JsonPath.parse(json.toString()).read("$");
        assertEquals(1, jsons.size());

        json = client.sendGetAndParse(resourceUrlNoSlash() + "?category=wrong");
        jsons = JsonPath.parse(json.toString()).read("$");
        assertEquals(0, jsons.size());
    }

    @Test
    void shouldGetServicesWithOffsetAndLimit() {
        client.sendPost(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        client.sendPost(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        var fourth = client.sendPostAndParse(resourceUrlNoSlash(), ResourceReader.getLogicalResource());
        var resources = client.sendGetAndParse(resourceUrlNoSlash() + "?offset=3&limit=2");

        assertEquals(1, resources.size());
        assertEquals(fourth, resources.get(0));
    }
}
