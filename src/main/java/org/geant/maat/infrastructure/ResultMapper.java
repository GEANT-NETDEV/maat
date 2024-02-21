package org.geant.maat.infrastructure;

import io.vavr.control.Either;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface ResultMapper {
    default ResponseEntity<?> foldResultWithStatus(Either<DomainError, ?> result, HttpStatus status) {
        return result.fold(DomainError::toResponseEntity, o -> ResponseEntity.status(status).body(o));
    }
}
