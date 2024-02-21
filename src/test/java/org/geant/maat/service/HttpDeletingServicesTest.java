package org.geant.maat.service;

import com.jayway.jsonpath.JsonPath;
import org.geant.maat.utils.ServiceReader;
import org.geant.maat.utils.ServiceHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpDeletingServicesTest extends HttpTest {

    private final ServiceHttpClient client = new ServiceHttpClient();
    private String serviceId;

    @BeforeAll
    void init() {
        var response = client.sendPost(serviceUrlNoSlash(), ServiceReader.getLogicalService());
        assertEquals(response.statusCode(), HttpStatus.CREATED.value());
        serviceId = JsonPath.parse(response.body()).read("$.id");
    }

    @Test
    @DisplayName("When deleting existing service by id should get 204")
    void deleteById() {
        var response = client.sendDelete(serviceUrl() + serviceId);
        assertEquals(HttpStatus.NO_CONTENT.value(), response.statusCode());
    }

    @Test
    @DisplayName("When deleting nonexistent service by id should get 404")
    void deleteByWrongId() {
        var response = client.sendDelete(serviceUrl() + "1234");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
    }

}
