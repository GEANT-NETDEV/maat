package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;


public class SchemedJson {

    private final JsonNode json;

    SchemedJson(JsonNode json) {
        this.json = json;
    }

    public static Either<String, org.geant.maat.service.SchemedJson> from(JsonNode json) {

        if (!json.has(ServiceProps.SCHEMA_LOCATION.name)) {
            return Either.left(String.format("Schemed json need to have %s property", ServiceProps.SCHEMA_LOCATION));
        }
        if (!json.has(ServiceProps.TYPE.name)) {
            return Either.left(String.format("Schemed json need to have %s property", ServiceProps.TYPE));
        }

        return Either.right(new org.geant.maat.service.SchemedJson(json));
    }

    public String getSchemaLocation() {
        return json.get(ServiceProps.SCHEMA_LOCATION.name).asText();
    }

    public String getType() {
        return json.get(ServiceProps.TYPE.name).asText();
    }

    public JsonNode toJson() {
        return json.deepCopy();
    }
}
