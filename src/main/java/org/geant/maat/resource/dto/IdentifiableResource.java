package org.geant.maat.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class IdentifiableResource extends BaseResource{
    private final String id;

    public IdentifiableResource(JsonNode state) {
        super(state);
        if (!state.has("id")) {
            throw new IllegalArgumentException(("'id' property not set"));
        }
        this.id = getProperty("id").get().asText();

    }

    public String getId() {
        return id;
    }
}
