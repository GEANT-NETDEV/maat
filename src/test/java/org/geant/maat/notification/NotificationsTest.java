package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.geant.maat.notification.dto.EventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class NotificationsTest extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private final ObjectMapper mapper = new ObjectMapper();
    private NotificationService notificationService;
    private CountingNotifier notifier;

    @BeforeEach
    void init() {
        String mongoConnectionData = String.format("mongodb://admin:abc123@localhost");
        notifier = new CountingNotifier();
        notificationService = new NotificationService(mongoConnectionData, notifier, "testListeners");
    }

    @Test
    void addListener() throws JsonProcessingException {
        var result = notificationService.addListener(dummyDto());

        assertTrue(result.isRight());
    }

    @Test
    void registerEvent() {
        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, null));
    }

    @Test
    @DisplayName("When listener registered with null query then should be notified on all events")
    void listenerWithNullQuery() throws JsonProcessingException {
        notificationService.addListener(dummyDto());

        int i = 0;
        for (var type : EventType.values()) {
            i++;
            notificationService.registerNewEventForTests(new EventDto(type, mapper.createObjectNode()));
            assertEquals(i, notifier.getSentNotificationsCount());
        }
    }

    @Test
    @DisplayName("When listener register with query then should be notified only with events that match that query")
    void listenerWithQuery() throws JsonProcessingException {
        var query = "eventType=ResourceDeleteEvent";
        var wrongEvent = new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode());
        var goodEvent = new EventDto(EventType.ResourceDeleteEvent, mapper.createObjectNode());
        notificationService.addListener(dummyDto(query));

        notificationService.registerNewEventForTests(wrongEvent);
        assertEquals(0, notifier.getSentNotificationsCount());

        notificationService.registerNewEventForTests(goodEvent);
        assertEquals(1, notifier.getSentNotificationsCount());
    }

    @Test
    @DisplayName("When trying to delete listener with wrong id should get error")
    void deleteListenerWrongId() throws JsonProcessingException {
        notificationService.addListener(dummyDto());
        var response = notificationService.deleteListener("wrong_id");
        assertTrue(response.isLeft());
    }

    @Test
    @DisplayName("Should be able to delete listener")
    void deleteListener() throws JsonProcessingException {
        var listener = notificationService.addListener(dummyDto());
        var id = listener.get().id();
        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceDeleteEvent, mapper.createObjectNode()));
        assertEquals(1, notifier.getSentNotificationsCount());

        var response = notificationService.deleteListener(id);
        assertTrue(response.isRight());

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceDeleteEvent, mapper.createObjectNode()));
        assertEquals(1, notifier.getSentNotificationsCount());
    }

    private CreateListenerDto dummyDto(String query) {
        try {
            return new CreateListenerDto(new URL("http://example.com"), query);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private CreateListenerDto dummyDto() {
        return dummyDto(null);
    }
}