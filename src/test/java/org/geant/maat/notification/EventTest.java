package org.geant.maat.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geant.maat.notification.dto.EventDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseProvidedChangedByUser() {
        Event event = Event.from(new EventDto(EventType.ResourceCreateEvent, objectMapper.createObjectNode()), "adamski");

        assertEquals("adamski", event.changedByUser());
    }

    @Test
    void shouldUseUnknownForLegacyFactoryMethod() {
        Event event = Event.from(new EventDto(EventType.ResourceCreateEvent, objectMapper.createObjectNode()));

        assertEquals("unknown", event.changedByUser());
    }
}
