package org.geant.maat.service;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import java.time.OffsetDateTime;


import java.util.UUID;

public class ServiceCreator {

    private final ServiceValidator validator;
    private final ServiceHrefBuilder hrefBuilder;

    ServiceCreator(ServiceHrefBuilder hrefBuilder) {
        this.validator = new ServiceValidator();
        this.hrefBuilder = hrefBuilder;
    }

    public Either<DomainError, Service> create(JsonNode json) {
        json = addMandatoryProperties(json);

        return validator.validate(json).map(Service::new);
    }

    private JsonNode addMandatoryProperties(JsonNode json) {
        ObjectNode objectNode = json.deepCopy();
        String id = UUID.randomUUID().toString();
        String serviceDate = OffsetDateTime.now().toString();

        objectNode.put("id", id);
        objectNode.put("href", hrefBuilder.id(id));
        objectNode.put("serviceDate", serviceDate);
        return objectNode;
    }

}
