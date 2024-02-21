package org.geant.maat.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URL;

public record CreateListenerDto(@NotNull(message = "callback must be valid url") URL callback, String query) {
}
