package org.geant.maat.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.stream.Stream;

public class Resource {
    private final JsonNode state;

    public Resource(JsonNode state) {
        Stream.of("id", "href").filter(mandatoryProperty -> state.get(mandatoryProperty) == null).forEach(property -> {
            throw new IllegalArgumentException(String.format("'%s' property not set", property));
        });
        this.state = state;
    }

    public static Resource from(String json) {
        try {
            return new Resource(new ObjectMapper().readTree(json));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException();
        }
    }

    public Optional<JsonNode> getProperty(String name) {
        JsonNode property = state.get(name);
        return property == null ? Optional.empty() : Optional.of(property);
    }

    public String getId() {
        return state.get("id").asText();
    }

    public String getHref() {
        return state.get("href").asText();
    }

    public JsonNode toJson() {
        return state.deepCopy();
    }
}


