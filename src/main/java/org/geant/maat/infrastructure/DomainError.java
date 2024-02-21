package org.geant.maat.infrastructure;

import org.springframework.http.ResponseEntity;

public record DomainError(String message, DomainErrorMapper errorCode) {

    public static DomainError fromExceptionWhileAction(DomainErrorMapper error, Exception exception, String action) {
        return new DomainError(String.format("Error occurred while '%s', because: %s", action, exception.getMessage()),
                               error);
    }

    public ResponseEntity<ErrorEntity> toResponseEntity() {
        return ResponseEntity.status(errorCode.toHttpStatus()).body(new ErrorEntity(message));
    }
}
