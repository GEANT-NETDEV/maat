package org.geant.maat.service;

import com.jayway.jsonpath.JsonPath;
import org.geant.maat.utils.ServiceHttpClient;
import org.geant.maat.utils.ServiceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
public class HttpUpdatingResourcesTest extends HttpTest {

    private final ServiceHttpClient client = new ServiceHttpClient();
    private String serviceId;

    @BeforeEach
    void init() {
        var response = client.sendPost(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        serviceId = JsonPath.parse(response.body()).read("$.id");
    }

    @Test
    @DisplayName("When updating service should get 200 and whole object with changes")
    void deleteById() {
        var response = client.sendPatchAndParse(serviceUrl() + serviceId, "{\"category\": \"xyz\"}");
        assertEquals("xyz", response.get("category").asText());
    }

    @Test
    @DisplayName("When trying to update forbidden properties should get 409")
    void deleteByWrongId() {
        var response = client.sendPatch(serviceUrl() + serviceId, "{\"id\": \"1234\"}");
        assertEquals(HttpStatus.CONFLICT.value(), response.statusCode());
    }

}
