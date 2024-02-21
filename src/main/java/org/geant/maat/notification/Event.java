package org.geant.maat.notification;

import org.geant.maat.notification.dto.EventDto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

record Event(String eventId, String eventTime, EventType eventType, Payload event) {
    public static Event from(EventDto dto) {
        var id = UUID.randomUUID().toString();
        String time = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString().replace("Z","");

        return new Event(id, time, dto.type(), new Payload(dto.payload()));
    }
}
