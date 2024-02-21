package org.geant.maat.notification;

import org.geant.maat.infrastructure.DomainErrorMapper;

enum Error implements DomainErrorMapper {
    ID_MISSING,
    PARAMETER_FORMAT_ERROR,
    IMPLEMENTATION_ERROR,
    DB_CONNECTION_ERROR;

    public int toHttpStatus() {
        return switch (this) {
            case ID_MISSING -> 404;
            case PARAMETER_FORMAT_ERROR -> 400;
            case DB_CONNECTION_ERROR -> 500;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }
}
