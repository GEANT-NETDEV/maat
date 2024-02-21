package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.geant.maat.utils.ServiceHttpClient;
import org.geant.maat.utils.ServiceReader;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpReadingServicesTest extends HttpTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceHttpClient client = new ServiceHttpClient();

    private String serviceId;
    private JsonNode createdService;
    private final ServiceRepository repository;

    @Autowired
    HttpReadingServicesTest(ServiceRepository repository) {this.repository = repository;}

    @BeforeEach
    void init() throws IOException {
        var response = client.sendPost(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        serviceId = JsonPath.parse(response.body()).read("$.id");
        createdService = mapper.readTree(response.body());
    }

    @AfterEach
    void clear() {
        repository.clean();
    }

    @Test
    @DisplayName("It should be able to get service by id")
    void getById() {
        var json = client.sendGetAndParse(serviceUrl() + serviceId);

        String id = JsonPath.parse(json.toString()).read("$.id");
        assertEquals(serviceId, id);
    }

    @Test
    @DisplayName("It should be able to get list of all services")
    void getAll() {
        var jsons = client.sendGetAndParse(serviceUrlNoSlash());

        for (var json : jsons) {
            assertDoesNotThrow(() -> (new Service(json)));
        }
    }

    @Test
    @DisplayName("When passing list of fields in request then all other fields should be filtered out")
    void fields() {
        var json = client.sendGetAndParse(serviceUrl()+ serviceId + "?fields=id,href");
        var expectedNames = new HashSet<>(Arrays.asList("id", "href"));

        json.fieldNames().forEachRemaining(name -> {
            assertTrue(expectedNames.contains(name), String.format("'%s' was not filtered out", name));
            expectedNames.remove(name);
        });
        assertEquals(0, expectedNames.size());
    }

    @Test
    @DisplayName("When property in list of fields does not exists in service then nothing should be returned for it")
    void fieldIncorrect() {
        var json = client.sendGetAndParse(serviceUrl() + serviceId + "?fields=bad_name,id");

        assertTrue(json.has("id"));
        assertFalse(json.has("bad_name"));
    }

    @Test
    @DisplayName("When no fields passed then all fields should be returned")
    void noFields() {
        var json = client.sendGetAndParse(serviceUrl() + serviceId);

        assertEquals(createdService, json);
    }

    @Test
    @DisplayName("When filtering is present then only service matching criteria should be returned")
    void filtering() {
        var json = client.sendGetAndParse(serviceUrlNoSlash() + "?category=stp&@type=LogicalResource");
        List<JsonNode> jsons = JsonPath.parse(json.toString()).read("$");
        assertEquals(1, jsons.size());

        json = client.sendGetAndParse(serviceUrlNoSlash() + "?category=wrong");
        jsons = JsonPath.parse(json.toString()).read("$");
        assertEquals(0, jsons.size());
    }

    @Test
    void shouldGetServicesWithOffsetAndLimit() {
        client.sendPost(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        client.sendPost(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        var fourth = client.sendPostAndParse(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        var services = client.sendGetAndParse(serviceUrlNoSlash() + "?offset=3&limit=2");

        assertEquals(1, services.size());
        assertEquals(fourth, services.get(0));
    }

}
