package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServiceService {

    Optional<Service> getService(String id);

    Either<DomainError, JsonNode> createService(JsonNode json, Boolean registerNewEventFlag);

    Either<DomainError, JsonNode> getService(String id, Collection<String> propsToFilter);

    Either<DomainError, String> deleteService(String id, Boolean registerNewEventFlag);

    Either<DomainError, JsonNode> updateService(String id, JsonNode updateJson, Boolean registerNewEventFlag);

    Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering);

    Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering, int offset, int limit);
}
