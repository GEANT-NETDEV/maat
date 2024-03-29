package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainErrorMapper;

enum Error implements DomainErrorMapper {
    VALIDATION_ERROR,
    SCHEMA_ERROR,
    RESOURCE_MISSING,
    FORBIDDEN_PROPERTIES,
    BAD_CATEGORY,
    RELATIONSHIP_ERROR,
    SERVICE_MISSING;

    public int toHttpStatus() {
        return switch (this) {
            case RESOURCE_MISSING -> 404;
            case FORBIDDEN_PROPERTIES -> 409;
            default -> 400;
        };
    }
}
