package org.geant.maat.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
public class ReferableService extends IdentifiableService {

    public ReferableService(JsonNode state) {
        super(state);
    }

    public String getHref() {
        return getId();
    }
}
