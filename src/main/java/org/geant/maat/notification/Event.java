package org.geant.maat.notification;

import org.geant.maat.notification.dto.EventDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

record Event(String eventId, String eventTime, EventType eventType, Payload event, String changedByUser) {
    public static Event from(EventDto dto) {
        var id = UUID.randomUUID().toString();
        String time = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString().replace("Z","");
        String currentUser = getCurrentUser();

        return new Event(id, time, dto.type(), new Payload(dto.payload()), currentUser);
    }

    private static String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return null;
    }
}
