package org.geant.maat.notification;

import org.geant.maat.notification.dto.EventDto;
import org.geant.maat.utils.ResourceReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryTest {
    private final String query = "eventType=ResourceCreateEvent&event.troubletTicket.severity=Urgent&" +
            "fields=event.troubleTicket.id, event.TroubleTicket.name, event.troubleTicket.severity";

    @Test
    void shouldMatchEventType() {
        var q = Query.from(query);
        assertTrue(q.isRight());
        var query = q.get();
        assertFalse(query.matchesEvent(
                Event.from(new EventDto(EventType.ResourceDeleteEvent, ResourceReader.getDefaultResource()))));
        assertTrue(query.matchesEvent(
                Event.from(new EventDto(EventType.ResourceCreateEvent, ResourceReader.getDefaultResource()))));
    }

}