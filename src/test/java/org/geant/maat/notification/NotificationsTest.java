package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.geant.maat.notification.dto.EventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void registerEventShouldIncludeChangedByUser() throws JsonProcessingException {
        var capturingNotifier = new CapturingNotifier();
        String mongoConnectionData = String.format("mongodb://admin:abc123@localhost");
        notificationService = new NotificationService(mongoConnectionData, capturingNotifier, "testListeners", () -> "adamski");
        notificationService.addListener(dummyDto());

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode()));

        assertEquals("adamski", capturingNotifier.lastEvent.changedByUser());
    }

    @Test
    void registerEventShouldReuseSameEventForAllListeners() throws JsonProcessingException {
        var capturingNotifier = new MultiCaptureNotifier();
        String mongoConnectionData = String.format("mongodb://admin:abc123@localhost");
        notificationService = new NotificationService(mongoConnectionData, capturingNotifier, "testListeners");
        notificationService.addListener(dummyDto());
        notificationService.addListener(dummyDto("eventType=ResourceCreateEvent"));

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode()));

        assertEquals(2, capturingNotifier.events.size());
        assertEquals(capturingNotifier.events.get(0).eventId(), capturingNotifier.events.get(1).eventId());
    }

    @Test
    void registerEventShouldWriteDispatchLogs(CapturedOutput output) throws JsonProcessingException {
        notificationService.addListener(dummyDto());

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode()));

        assertTrue(output.getOut().contains("Registering test event id="));
        assertTrue(output.getOut().contains("Dispatching test event id="));
        assertTrue(output.getOut().contains("Sending event id="));
        assertTrue(output.getOut().contains("dispatch started for listener"));
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

    private static class CapturingNotifier extends Notifier {
        private Event lastEvent;

        @Override
        io.vavr.control.Either<String, String> sendNotification(Listener listener, Event event) {
            this.lastEvent = event;
            return io.vavr.control.Either.right("OK");
        }
    }

    private static class MultiCaptureNotifier extends Notifier {
        private final List<Event> events = new ArrayList<>();

        @Override
        Either<String, String> sendNotification(Listener listener, Event event) {
            events.add(event);
            return Either.right("OK");
        }
    }
}
