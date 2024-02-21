package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class ResourceFinder {
    private final ResourceRepository repository;


    ResourceFinder(ResourceRepository repository) {this.repository = repository;}

    Collection<JsonNode> find(List<String> fields, Map<String, String> filtering) {
        return repository.findAll(fields, filtering);
    }

    Collection<JsonNode> find(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        return repository.findAll(fields, filtering, offset, limit);
    }
}
