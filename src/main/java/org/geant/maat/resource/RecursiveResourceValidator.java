package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.schema.validator.ValidationContext;

import java.net.MalformedURLException;
import java.net.URL;

class RecursiveResourceValidator extends ResourceValidator {

    @Override
    ValidationContext<OAI3> createContext(JsonNode schema) throws ResolutionException, MalformedURLException {
        return new ValidationContext<>(new OAI3Context(new URL("file:/"), schema));
    }

    public Either<DomainError, JsonNode> validate(JsonNode resource) {
        var basicValidation = super.validate(resource);
        if (basicValidation.isLeft()) {
            return basicValidation;
        }

        var fields = resource.fields();
        while (fields.hasNext()) {
            var child = fields.next();
            if (child.getValue().has("@schemaLocation")) {
                var result = validate(child.getValue());
                if (result.isLeft()) {
                    return Either.left(new DomainError(String.format("In '%s' found error(s):\n%s", child.getKey(),
                                                                     result.getLeft()),
                                                       Error.VALIDATION_ERROR));
                }
            }
        }
        return Either.right(resource);
    }
}
