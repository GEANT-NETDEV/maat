package org.geant.maat.common;

import java.util.Collection;
import java.util.Map;

public record KeycloakUserContext(
        String username,
        Map<String, Object> userAccessFilters,
        Collection<String> maatClientRoles
) {
}
