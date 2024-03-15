package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


class ResourceCreator {
    private final ResourceValidator validator;
    private final ResourceHrefBuilder hrefBuilder;

    ResourceCreator(ResourceHrefBuilder hrefBuilder) {
        this.validator = new ResourceValidator();
        this.hrefBuilder = hrefBuilder;
    }

    public Either<DomainError, Resource> create(JsonNode json) {
        json = addMandatoryProperties(json);

        return validator.validate(json).map(Resource::new);
    }

    private JsonNode addMandatoryProperties(JsonNode json) {
        ObjectNode objectNode = json.deepCopy();
        String id = UUID.randomUUID().toString();

        objectNode.put("id", id);
        objectNode.put("href", hrefBuilder.id(id));
        return objectNode;
    }
}
