package org.geant.maat.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Option;
public class BaseService {

    private final JsonNode state;

    public BaseService(JsonNode state) {
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
