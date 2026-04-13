package org.geant.maat.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geant.maat.notification.dto.EventDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@ExtendWith(OutputCaptureExtension.class)
class HttpSendNotifierTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpNotifier notifier;
    private TestListener listener;
    private Listener registeredListener;

    @BeforeEach
    void init() throws IOException {
        notifier = new HttpNotifier();
        listener = new TestListener(9999);
        registeredListener = new Listener("listener-1", listener.address, Query.from(null).get());
    }

    @AfterEach
    void stopListener() {
        listener.stop();
    }

    @Test
    @DisplayName("When http Notifier used listener should be notified")
    void httpNotifier() throws InterruptedException, ExecutionException {
        var event = Executors.newSingleThreadExecutor().submit(() -> listener.listenForOneMessage(1));

        notifier.notifyListener(registeredListener, Event.from(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode())));

        assertTrue(event.get().isDefined());
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Events send by http notifier should have ")
    @TestFactory
    Stream<DynamicTest> httpNotifierStructure() throws IOException, InterruptedException, ExecutionException {
        var event = Executors.newSingleThreadExecutor().submit(() -> listener.listenForOneMessage(1));

        notifier.notifyListener(registeredListener, Event.from(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode())));
        var json = mapper.readTree(event.get().get());
        Thread.sleep(200);

        return Stream.of("eventId", "eventTime", "eventType", "event", "changedByUser")
                .map(p -> dynamicTest(p, () -> json.has(p)));
    }

    @Test
    void httpNotifierShouldLogCallbackResponse(CapturedOutput output) throws InterruptedException, ExecutionException {
        var event = Executors.newSingleThreadExecutor().submit(() -> listener.listenForOneMessage(1));

        notifier.notifyListener(registeredListener, Event.from(new EventDto(EventType.ResourceCreateEvent, mapper.createObjectNode())));
        assertTrue(event.get().isDefined());

        Thread.sleep(300);

        assertTrue(output.getOut().contains("EVENT_DELIVERED"));
        assertTrue(output.getOut().contains("listenerId=listener-1"));
        assertTrue(output.getOut().contains("callback=http://localhost:9999"));
        assertTrue(output.getOut().contains("statusCode=200"));
        assertTrue(output.getOut().contains("body=OK"));
    }
}
