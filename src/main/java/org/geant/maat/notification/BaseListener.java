package org.geant.maat.notification;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Option;

public class BaseListener {
    private final JsonNode state;

    public BaseListener(JsonNode state) { this.state = state;}

    public Option<JsonNode> getProperty(String name) {
        JsonNode property = state.get(name);
        return Option.of(property);
    }

    public JsonNode toJson() { return state.deepCopy();}

}
