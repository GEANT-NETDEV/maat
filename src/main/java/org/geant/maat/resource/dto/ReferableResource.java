package org.geant.maat.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class ReferableResource extends IdentifiableResource{
    public ReferableResource(JsonNode state) {
        super(state);
    }

    public String getHref() {
        return getId();
    }
}
