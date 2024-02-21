package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;

import java.util.*;
import java.util.function.Predicate;
class InMemoryServiceRepository implements ServiceRepository {

    private final Map<String, Service> services = new HashMap<>();

    @Override
    public Optional<Service> find(String id) {
        Service service = services.get(id);
        return service == null ? Optional.empty() : Optional.of(service);
    }

    @Override
    public Either<DomainError, JsonNode> find(String id, Collection<String> propsToFilter) {
        var service = services.get(id);
        if (service == null) {
            return Either.left(
                    new DomainError(String.format("Service with id '%s' does not exist", id),
                            Error.SERVICE_MISSING));
        }
        if (propsToFilter.isEmpty()) {
            return Either.right(service.toJson());
        }

        return Either.right(jsonWithFields(service.toJson(), propsToFilter));
    }

    private JsonNode jsonWithFields(JsonNode json, Collection<String> fields) {
        if (fields.isEmpty()) {
            return json;
        }
        ObjectNode on = new ObjectMapper().createObjectNode();
        for (String field : fields) {
            if (json.has(field)) {
                on.set(field, json.get(field));
            }
        }
        return on;
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering) {
        Predicate<JsonNode> matchesAllFieldFilters = (json) -> {
            for (var entry : filtering.entrySet()) {
                var key = entry.getKey();
                if (!json.has(key) || !json.get(key).asText().equals(entry.getValue())) {
                    return false;
                }
            }
            return true;
        };

        return services.values()
                .stream()
                .map(Service::toJson)
                .filter(matchesAllFieldFilters)
                .map(json -> jsonWithFields(json, fields))
                .toList();
    }

    @Override
    public Either<DomainError, Service> save(Service service) {
        services.put(service.getId(), service);
        return Either.right(service);
    }

    @Override
    public Either<DomainError, String> delete(String id) {
        return services.remove(id) == null ?
                Either.left(new DomainError(String.format("Service with id '%s' does not exist", id),
                        Error.SERVICE_MISSING)) :
                Either.right(id);
    }

    @Override
    public Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        services.put(id, new Service(updateJson));
        return Either.right(updateJson);
    }

    @Override
    public void clean() {
        services.clear();
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        return findAll(fields, filtering).stream().skip(offset).limit(limit).toList();
    }

}
