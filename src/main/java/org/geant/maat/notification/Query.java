package org.geant.maat.notification;

import org.geant.maat.infrastructure.DomainError;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

class Query {
    final Collection<String> fields;
    final Option<EventType> eventType;

    private Query(Collection<String> fields, EventType eventType) {
        this.fields = fields;
        this.eventType = Option.of(eventType);
    }

    static Either<DomainError, Query> from(String query) {
        if (query == null) {
            return Either.right(new Query(new ArrayList<>(), null));
        }
        var queryParts = UriComponentsBuilder.fromUriString("")
                .query(query)
                .build()
                .getQueryParams();
        var fields = parseFields(queryParts.getFirst("fields"));
        var maybeType = parseEventType(queryParts);

        return maybeType.map(type -> new Query(fields, type));
    }

    private static Collection<String> parseFields(String chunk) {
        var fields = new ArrayList<String>();
        if (chunk == null) {
            return fields;
        }

        for (var name : chunk.split(",")) {
            fields.add(name.trim());
        }
        return fields;
    }

    private static Either<DomainError, EventType> parseEventType(MultiValueMap<String, String> map) {
        for (var entry : map.entrySet()) {
            if (entry.getKey().equals("eventType")) {
                var values = entry.getValue();
                if (values.size() == 0) {
                    return Either.left(new DomainError("No value for 'eventType'",
                                                       Error.PARAMETER_FORMAT_ERROR));
                }
                var eventName = values.get(0);
                for (var type : EventType.values()) {
                    if (type.name().equals(eventName)) {
                        return Either.right(type);
                    }
                }
                return Either.left(
                        new DomainError("Given event type is invalid. Event types available: " + Arrays.toString(
                                EventType.values()), Error.PARAMETER_FORMAT_ERROR));
            }
        }
        return Either.left(
                new DomainError("eventType field is missing", Error.PARAMETER_FORMAT_ERROR));
    }

    public boolean matchesEvent(Event event) {
        return eventType.map(type -> type == event.eventType()).getOrElse(true);
    }
}
