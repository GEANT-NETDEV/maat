package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

class ResourceUpdater {
    private final ResourceRepository repository;
    private final ResourceValidator validator;

    public ResourceUpdater(ResourceRepository repository) {
        this.repository = repository;
        this.validator = new ResourceValidator();
    }

    Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        var forbiddenProps = listForbiddenProps(updateJson);
        if (!forbiddenProps.isEmpty()) {
            return Either.left(new DomainError(String.format("Found forbidden props: %s", forbiddenProps),
                                               Error.FORBIDDEN_PROPERTIES));
        }

        var optionalResource = repository.find(id);
        if (optionalResource.isEmpty()) {
            return Either.left(new DomainError(String.format("Resource with id %s missing", id),
                                               Error.RESOURCE_MISSING));
        }
        var resource = (ObjectNode) optionalResource.get().toJson();
        updateJson.fieldNames().forEachRemaining(prop -> {
            if (updateJson.get(prop).isNull()) {
                resource.remove(prop);
            } else {
                resource.set(prop, updateJson.get(prop));
            }
        });

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        resource.put("lastUpdateDate", updateDate);

        return validator.validate(resource).flatMap(jsonNode -> repository.update(id, jsonNode));
    }

    private Collection<String> listForbiddenProps(JsonNode json) {
        var forbiddenProps = Arrays.asList("id", "href", "@schemaLocation", "@type", "startOperatingDate", "lastUpdateDate");
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
