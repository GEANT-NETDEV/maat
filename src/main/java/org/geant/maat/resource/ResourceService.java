package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResourceService {

    Optional<Resource> getResource(String id);

    Either<DomainError, JsonNode> createResource(JsonNode json, Boolean registerNewEventFlag);

    Either<DomainError, JsonNode> getResource(String id, Collection<String> propsToFilter);

    Either<DomainError, String> deleteResource(String id, Boolean registerNewEventFlag);

    Either<DomainError, JsonNode> updateResource(String id, JsonNode updateJson, Boolean registerNewEventFlag);

    Collection<JsonNode> getResources(List<String> fields, Map<String, String> filtering, String sort);

    Collection<JsonNode> getResources(
            List<String> fields, Map<String, String> filtering, int offset, int limit, String sort);


}
