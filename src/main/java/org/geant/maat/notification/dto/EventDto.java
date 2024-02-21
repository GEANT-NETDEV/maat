package org.geant.maat.notification.dto;

import org.geant.maat.notification.EventType;
import com.fasterxml.jackson.databind.JsonNode;

public record EventDto(EventType type, JsonNode payload) {
}
