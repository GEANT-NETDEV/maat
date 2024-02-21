package org.geant.maat.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Option;

public class BaseResource {
    private final JsonNode state;

    public BaseResource(JsonNode state) {
        this.state = state;
    }

    public Option<JsonNode> getProperty(String name) {
        JsonNode property = state.get(name);
        return Option.of(property);
    }

    public JsonNode toJson() {
        return state.deepCopy();
    }
}
