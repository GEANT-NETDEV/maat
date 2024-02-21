package org.geant.maat.notification;

import org.geant.maat.notification.dto.CreateListenerDto;
import org.geant.maat.notification.dto.EventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class HttpSendNotifierTest extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private final ObjectMapper mapper = new ObjectMapper();
    private NotificationService notificationService;
    private TestListener listener;

    @BeforeEach
    void init() throws IOException {
        String mongoConnectionData = String.format("mongodb://admin:abc123@localhost");
        notificationService = new NotificationService(mongoConnectionData, new HttpNotifier(),"testListeners");
        listener = new TestListener(9999);
        notificationService.addListener(new CreateListenerDto(listener.address, null));
    }

    @AfterEach
    void stopListener() {
        listener.stop();
    }

    @Test
    @DisplayName("When http Notifier used listener should be notified")
    void httpNotifier() throws InterruptedException, ExecutionException {
        var event = Executors.newSingleThreadExecutor().submit(() -> listener.listenForOneMessage(1));

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode()));

        assertTrue(event.get().isDefined());
    }

    @Test
    @DisplayName("Events send by http notifier should have ")
    @TestFactory
    Stream<DynamicTest> httpNotifierStructure() throws IOException, InterruptedException, ExecutionException {
        var event = Executors.newSingleThreadExecutor().submit(() -> listener.listenForOneMessage(1));

        notificationService.registerNewEventForTests(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode()));
        var json = mapper.readTree(event.get().get());

        return Stream.of("eventId", "eventTime", "eventType", "event").map(p -> dynamicTest(p, () -> json.has(p)));
    }
}
