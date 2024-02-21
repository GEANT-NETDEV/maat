package org.geant.maat.integration.notification;

import org.geant.maat.resource.HttpTest;
import org.geant.maat.utils.ResourceHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
class HttpNotificationsTest extends HttpTest {
    private ResourceHttpClient client;

    @BeforeEach
    void init() {
        client = new ResourceHttpClient();
    }

    @Test
    void whenRegisteringListenerWithOkCallbackAndQueryEmpty_shouldReturn201() throws JsonProcessingException {
        var response = client.sendPost(notificationUrl(), "{\"callback\": \"http://in.listener.com\"}");
        assertEquals(HttpStatus.CREATED.value(), response.statusCode());

        var json = new ObjectMapper().readTree(response.body());
        assertTrue(json.has("id"));
        assertEquals("http://in.listener.com", json.get("callback").asText());
        assertTrue(json.get("query").isNull());
    }

    @Test
    void whenRegisteringWithoutCallback_thenShouldGet400() {
        var response = client.sendPost(notificationUrl(), "{}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    @Test
    void whenRegisteringWithValidEventTypeInQuery_thenShouldGetItInResponse() throws JsonProcessingException {
        var response = client.sendPost(notificationUrl(),
                                       "{\"callback\": \"http://in.listener.com\", \"query\":\"eventType=ResourceCreateEvent\"}");
        assertEquals(HttpStatus.CREATED.value(), response.statusCode());

        var json = new ObjectMapper().readTree(response.body());
        assertTrue(json.get("query").asText().contains("eventType=ResourceCreateEvent"));
    }

    @Test
    void whenRegisteringWithInvalidEventTypeInQuery_thenShouldGet400() {
        var response = client.sendPost(notificationUrl(),
                                       "{\"callback\": \"http://in.listener.com\", \"query\":\"eventType=invalid\"}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    @Test
    void whenDeleteWrongId_shouldGet404() {
        var response = client.sendDelete(notificationUrl() + "/id");
        assertEquals(404, response.statusCode());
    }

    @Test
    void whenDeletingById_shouldGet204() {
        var id = client.sendPostAndParse(notificationUrl(), "{\"callback\": \"http://in.listener.com\"}")
                .get("id")
                .asText();
        var response = client.sendDelete(notificationUrl() + "/" + id);
        assertEquals(204, response.statusCode());
    }
}
