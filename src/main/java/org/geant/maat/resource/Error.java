package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainErrorMapper;

enum Error implements DomainErrorMapper {
    VALIDATION_ERROR,
    SCHEMA_ERROR,
    RESOURCE_MISSING,
    FORBIDDEN_PROPERTIES,
    BAD_CATEGORY,
    RELATIONSHIP_ERROR,
    SERVICE_MISSING,
    FILTER_ERROR;

    public int toHttpStatus() {
        return switch (this) {
            case RESOURCE_MISSING -> 404;
            case FORBIDDEN_PROPERTIES -> 409;
            case FILTER_ERROR -> 403;
            default -> 400;
        };
    }
}
