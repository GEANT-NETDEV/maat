package org.geant.maat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.geant.maat.utils.ServiceHttpClient;
import org.geant.maat.utils.ServiceReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
public class HttpCreatingServicesTest extends HttpTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceHttpClient client = new ServiceHttpClient();
    private final String example_service = ServiceReader.getLogicalService();

    @Test
    @DisplayName("When POSTing service then it should be created")
    public void create() {
        var response = client.sendPost(serviceUrlNoSlash(), example_service);
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("When trying to create service that does not match the schema then should get 404")
    void creatingIncorrect() throws IOException {
        ObjectNode on = (ObjectNode) mapper.readTree(example_service);
        on.put("name", 1234);

        var response = client.sendPost(serviceUrlNoSlash(), on.toString());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    @Test
    @DisplayName("GETting href from service creation response should return the same service")
    public void getByHref() {
        var response = client.sendPost(serviceUrlNoSlash(), example_service);
        String href = JsonPath.parse(response.body()).read("$.href");
        String id = JsonPath.parse(response.body()).read("$.id");
        response = client.sendGet(href);
        String new_id = JsonPath.parse(response.body()).read("$.id");

        assertEquals(response.statusCode(), HttpStatus.OK.value());
        assertEquals(id, new_id);
    }

    @Test
    @DisplayName("When trying to get service by id that does not exist then should get 404")
    void getNonExistingId() {
        var response = client.sendGet(serviceUrl() + "badId");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
    }
}
