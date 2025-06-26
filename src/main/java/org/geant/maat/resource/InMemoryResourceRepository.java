package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;

import java.util.*;
import java.util.function.Predicate;

class InMemoryResourceRepository implements ResourceRepository {
    private final Map<String, Resource> resources = new HashMap<>();

    @Override
    public Optional<Resource> find(String id) {
        Resource resource = resources.get(id);
        return resource == null ? Optional.empty() : Optional.of(resource);
    }

    @Override
    public Either<DomainError, JsonNode> find(String id, Collection<String> propsToFilter) {
        var resource = resources.get(id);
        if (resource == null) {
            return Either.left(
                    new DomainError(String.format("Resource with id '%s' does not exist", id),
                                    Error.RESOURCE_MISSING));
        }
        if (propsToFilter.isEmpty()) {
            return Either.right(resource.toJson());
        }

        return Either.right(jsonWithFields(resource.toJson(), propsToFilter));
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
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, String sort) {
        Predicate<JsonNode> matchesAllFieldFilters = json -> {
            for (var entry : filtering.entrySet()) {
                var key = entry.getKey();
                if (!json.has(key) || !json.get(key).asText().equals(entry.getValue())) {
                    return false;
                }
            }
            return true;
        };

        return resources.values()
                .stream()
                .map(Resource::toJson)
                .filter(matchesAllFieldFilters)
                .map(json -> jsonWithFields(json, fields))
                .toList();
    }

    @Override
    public Either<DomainError, Resource> save(Resource resource) {
        resources.put(resource.getId(), resource);
        return Either.right(resource);
    }

    @Override
    public Either<DomainError, String> delete(String id) {
        return resources.remove(id) == null ?
                Either.left(new DomainError(String.format("Resource with id '%s' does not exist", id),
                                            Error.RESOURCE_MISSING)) :
                Either.right(id);
    }

    @Override
    public Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        resources.put(id, new Resource(updateJson));
        return Either.right(updateJson);
    }

    @Override
    public void clean() {
        resources.clear();
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, int offset, int limit, String sort) {
        return findAll(fields, filtering, sort).stream().skip(offset).limit(limit).toList();
    }
}
