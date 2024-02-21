package org.geant.maat.service;

import org.geant.maat.infrastructure.DomainError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER;

class SchemaFetcher {

    public Either<DomainError, JsonNode> fetch(String stringUrl) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            return Either.left(DomainError.fromExceptionWhileAction(org.geant.maat.service.Error.SCHEMA_ERROR, e, "parsing url"));
        }
        try {
            return Either.right(mapper.readTree(url));
        } catch (IOException e) {
            return Either.left(DomainError.fromExceptionWhileAction(org.geant.maat.service.Error.SCHEMA_ERROR, e,
                    String.format("parsing schema from %s", stringUrl)));
        }
    }
}
