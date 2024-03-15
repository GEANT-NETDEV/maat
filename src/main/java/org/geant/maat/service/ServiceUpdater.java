package org.geant.maat.service;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
public class ServiceUpdater {

    private final ServiceRepository repository;

    private final ServiceValidator validator;

    public ServiceUpdater(ServiceRepository repository) {
        this.repository = repository;
        this.validator = new ServiceValidator();
    }

    Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        var forbiddenProps = listForbiddenProps(updateJson);
        if (!forbiddenProps.isEmpty()) {
            return Either.left(new DomainError(String.format("Found forbidden props: %s", forbiddenProps),
                    Error.FORBIDDEN_PROPERTIES));
        }

        var optionalService = repository.find(id);
        if (optionalService.isEmpty()) {
            return Either.left(new DomainError(String.format("Service with id %s missing", id),
                    Error.SERVICE_MISSING));
        }
        var service = (ObjectNode) optionalService.get().toJson();
        updateJson.fieldNames().forEachRemaining(prop -> {
            if (updateJson.get(prop).isNull()) {
                service.remove(prop);
            } else {
                service.set(prop, updateJson.get(prop));
            }
        });

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        service.put("lastUpdateDate", updateDate);

        return validator.validate(service).flatMap(jsonNode -> repository.update(id, jsonNode));
    }

    private Collection<String> listForbiddenProps(JsonNode json) {
        var forbiddenProps = Arrays.asList("id", "href", "@schemaLocation", "@type", "serviceDate", "lastUpdateDate");
        var fieldNames = json.fieldNames();
        var result = new ArrayList<String>();
        while (fieldNames.hasNext()) {
            var name = fieldNames.next();
            if (forbiddenProps.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }


}
