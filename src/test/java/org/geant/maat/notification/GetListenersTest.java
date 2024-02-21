package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetListenersTest extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private NotificationService notificationService;
    private String id;

    @BeforeEach
    void init() throws MalformedURLException, JsonProcessingException {
        String mongoConnectionData = String.format("mongodb://admin:abc123@localhost");
        notificationService = new NotificationService(mongoConnectionData, new CountingNotifier(), "testListeners");
        id = notificationService.addListener(new CreateListenerDto(new URL("http://abc"), null)).get().id();
    }


    @Test
    void itShouldBeAbleToGetListenersList() {
        var listeners = notificationService.getListeners();

        assertEquals(1, listeners.get().size());
    }

    @Test
    void itShouldBeAbleToGetListenerById() {
        var listener = notificationService.getListener(id).get();

        assertEquals(id, listener.id());
    }

    @Test
    void whenGettingListenerByWrongIdShouldGetError() {
        var listener = notificationService.getListener("wrong_id");

        assertTrue(listener.isLeft());
    }
}
