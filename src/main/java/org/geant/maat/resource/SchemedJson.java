package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;

class SchemedJson {
    private final JsonNode json;

    SchemedJson(JsonNode json) {
        this.json = json;
    }

    public static Either<String, SchemedJson> from(JsonNode json) {
        if (!json.has(ResourceProps.SCHEMA_LOCATION.name)) {
            return Either.left(String.format("Schemed json need to have %s property", ResourceProps.SCHEMA_LOCATION));
        }
        if (!json.has(ResourceProps.TYPE.name)) {
            return Either.left(String.format("Schemed json need to have %s property", ResourceProps.TYPE));
        }

        return Either.right(new SchemedJson(json));
    }

    public String getSchemaLocation() {
        return json.get(ResourceProps.SCHEMA_LOCATION.name).asText();
    }

    public String getType() {
        return json.get(ResourceProps.TYPE.name).asText();
    }

    public JsonNode toJson() {
        return json.deepCopy();
    }
}
