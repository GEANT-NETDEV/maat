package org.geant.maat.service;

import org.geant.maat.infrastructure.DomainErrorMapper;

enum Error implements DomainErrorMapper {
    VALIDATION_ERROR,
    SCHEMA_ERROR,
    SERVICE_MISSING,
    FORBIDDEN_PROPERTIES,
    BAD_CATEGORY,
    RELATIONSHIP_ERROR;
    public int toHttpStatus() {
        return switch (this) {
            case SERVICE_MISSING -> 404;
            case FORBIDDEN_PROPERTIES -> 409;
            default -> 400;
        };
    }
}