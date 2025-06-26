package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

interface ResourceRepository {
    Optional<Resource> find(String id);

    Either<DomainError, JsonNode> find(String id, Collection<String> propsToFilter);

    Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, String sort);

    Either<DomainError, Resource> save(Resource resource);

    Either<DomainError, String> delete(String id);

    Either<DomainError, JsonNode> update(String id, JsonNode updateJson);

    void clean();

    Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, int offset, int limit, String sort);
}
