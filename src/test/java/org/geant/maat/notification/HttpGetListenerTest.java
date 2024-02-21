package org.geant.maat.notification;


import org.geant.maat.MaatApplication;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.geant.maat.notification.dto.ListenerCreatedDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {MaatApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"security.require-ssl=false", "server.address=localhost", "server.port=12345", "resourceService.type=extended", "resourceService.checkExistingResource=true"})
class HttpGetListenerTest extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private final String endpoint = "http://localhost:12345/hub";

    @Test
    void shouldGetListOfListeners() {
        var id = createResource();
        var rt = new RestTemplate();
        var rtRequest = rt.getForEntity(endpoint, ListenerCreatedDto[].class);

        assertEquals(200, rtRequest.getStatusCode().value());
        assertTrue(Objects.requireNonNull(rtRequest.getBody()).length > 0);
        rt.delete(endpoint + "/" + id);
    }

    @Test
    void shouldGetListenerById() {
        var id = createResource();
        var rt = new RestTemplate();
        var rtRequest = new RestTemplate().getForEntity(endpoint + "/" + id, ListenerCreatedDto.class);

        assertEquals(200, rtRequest.getStatusCode().value());
        assertEquals(id, rtRequest.getBody().id());
        rt.delete(endpoint + "/" + id);
    }

    private String createResource() {
        ResponseEntity<ListenerCreatedDto> rt = null;
        try {
            rt = new RestTemplate().postForEntity(
                    endpoint,
                    new CreateListenerDto(new URL(endpoint), null),
                    ListenerCreatedDto.class);
        } catch (MalformedURLException e) {
            assert false : "Creating resource failed: " + e;
        }
        return rt.getBody().id();
    }
}
