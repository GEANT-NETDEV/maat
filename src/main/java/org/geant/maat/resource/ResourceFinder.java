package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class ResourceFinder {
    private final ResourceRepository repository;


    ResourceFinder(ResourceRepository repository) {this.repository = repository;}

    Collection<JsonNode> find(List<String> fields, Map<String, String> filtering, String sort) {
        return repository.findAll(fields, filtering, sort);
    }

    Collection<JsonNode> find(List<String> fields, Map<String, String> filtering, int offset, int limit, String sort) {
        return repository.findAll(fields, filtering, offset, limit, sort);
    }
}
