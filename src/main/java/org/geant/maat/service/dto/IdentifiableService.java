package org.geant.maat.service.dto;

import com.fasterxml.jackson.databind.JsonNode;


public class IdentifiableService extends BaseService {

    private final String id;

    public IdentifiableService(JsonNode state) {
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
