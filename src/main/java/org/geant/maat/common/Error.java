package org.geant.maat.common;

import org.geant.maat.infrastructure.DomainErrorMapper;

enum Error implements DomainErrorMapper {

    FILTER_ERROR;

    public int toHttpStatus() {
        return switch (this) {
            case FILTER_ERROR -> 403;
            default -> 400;
        };
    }
}

