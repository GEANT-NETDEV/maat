package org.geant.maat.resource;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.geant.maat.common.PropertiesLoader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.openapi4j.schema.validator.v3.ValidationOptions.ADDITIONAL_PROPS_RESTRICT;

class ResourceValidator {
    private final SchemaFetcher schemaFetcher;

    ResourceValidator() {
        this.schemaFetcher = new SchemaFetcher();
    }

    ValidationContext<OAI3> createContext(JsonNode schema) throws ResolutionException, MalformedURLException {
        ValidationContext<OAI3> context = new ValidationContext<>(new OAI3Context(new URL("file:/"), schema));

        String additionalPropsRestrict="";
        try {
            additionalPropsRestrict = PropertiesLoader.loadProperties("application.properties").getProperty("validator.resource.additional-props-restrict");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.setOption(ADDITIONAL_PROPS_RESTRICT, additionalPropsRestrict.equals("true"));
        return context;
    }

    private Either<DomainError, JsonNode> validate(JsonNode schema, JsonNode resource) {

        var schemedJson = SchemedJson.from(resource);
        if (schemedJson.isLeft()) {
            return Either.left(new DomainError(schemedJson.getLeft(), Error.VALIDATION_ERROR));
        }
        // TODO wrap schema with custom object
        if (!schema.has("definitions")) {
            return Either.left(new DomainError("definitions field missing in schema", Error.SCHEMA_ERROR));
        }

        String type = schemedJson.get().getType();
        if (!schema.get("definitions").has(type)) {
            return Either.left(new DomainError(String.format("Definition for resource type '%s' is missing", type),
                                               Error.SCHEMA_ERROR));
        }
        ValidationData<Void> validation = new ValidationData<>();
        SchemaValidator schemaValidator;

        try {
            schemaValidator = new SchemaValidator(createContext(schema), null, schema.get("definitions").get(type));
        } catch (ResolutionException | MalformedURLException e) {
            return Either.left(DomainError.fromExceptionWhileAction(Error.SCHEMA_ERROR, e, "creating schema context"));
        }

        JsonNode json = schemedJson.get().toJson();
        schemaValidator.validate(json, validation);
        return validation.isValid() ? Either.right(json) :
                Either.left(new DomainError(validation.results().toString(), Error.VALIDATION_ERROR));
    }

    public Either<DomainError, JsonNode> validate(JsonNode resource) {
        String validatorEnable="";
        try {
            validatorEnable = PropertiesLoader.loadProperties("application.properties").getProperty("validator.resource.schema");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final var maybeSchemedJson = SchemedJson.from(resource);

        if (maybeSchemedJson.isLeft()) {
            if(validatorEnable.equals("true")){
                if(maybeSchemedJson.getLeft().equals("Schemed json need to have SCHEMA_LOCATION property"))
                    return Either.left(new DomainError("Schemed json need to have @schemaLocation property", Error.VALIDATION_ERROR));
                else if(maybeSchemedJson.getLeft().equals("Schemed json need to have TYPE property"))
                    return Either.left(new DomainError("Schemed json need to have @type property", Error.VALIDATION_ERROR));}
            else
                return Either.right(resource);

        }
        var schemedJson = maybeSchemedJson.get();
        return fetchSchema(schemedJson).flatMap(schema -> validate(schema, resource));
    }

    private Either<DomainError, JsonNode> fetchSchema(SchemedJson json) {
        return schemaFetcher.fetch(json.getSchemaLocation());
    }


}
