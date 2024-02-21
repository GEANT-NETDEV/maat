package org.geant.maat.notification;

import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.dto.CreateListenerDto;
import io.vavr.control.Either;

import java.util.UUID;

class ListenerCreator {

    public Either<DomainError, Listener> createListener(CreateListenerDto dto) {
        String id = UUID.randomUUID().toString();

        return Query.from(dto.query())
                .map(query -> new Listener(id, dto.callback(), query));
    }

    public Either<DomainError, Listener> createListenerFromDb(CreateListenerDto dto, String id) {
        return Query.from(dto.query())
                .map(query -> new Listener(id, dto.callback(), query));
    }
}
