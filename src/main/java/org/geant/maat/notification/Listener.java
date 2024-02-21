package org.geant.maat.notification;

import org.geant.maat.notification.dto.ListenerCreatedDto;

import java.net.URL;

record Listener(String id, URL callback, Query query) {

    boolean wantsEvent(Event event) {
        return query.matchesEvent(event);
    }

     ListenerCreatedDto toListenerCreatedDto() {
        String stringQuery = null;
        if(!query.eventType.isEmpty()) {
            stringQuery = String.format("eventType=%s", query.eventType.get().name());
        }
        return new ListenerCreatedDto(id, callback.toString(), stringQuery);
    }
}
