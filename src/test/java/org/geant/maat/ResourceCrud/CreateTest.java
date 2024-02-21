package org.geant.maat.ResourceCrud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When creating resource from json")
class CreateTest extends ResourceTest {
    @Test
    @DisplayName("mandatory properties - 'id' and 'href' should be added")
    void shouldContainMandatory() {
        var resource = resourceService.createResource(validJson, false).get();

        assertTrue(resource.has("id"));
        assertTrue(resource.has("href"));
    }

    @Test
    @DisplayName("When @type missing should create resource without validation")
    void typeMissing() {
        corruptedCategoryJson.remove("@type");

        var newResource = resourceService.createResource(validJson, false);

        assertTrue(newResource.isRight());
    }

    @Test
    @DisplayName("When @schemaLocation missing should create resource without validation")
    void schemaLocationMissing() {
        corruptedCategoryJson.remove("@schemaLocation");

        var newResource = resourceService.createResource(validJson, false);

        assertTrue(newResource.isRight());
    }


    @Test
    @DisplayName("When @type and @schemaLocation are present and json matches the schema then it should be created")
    void goodResource() {
        var resource = resourceService.createResource(validJson, false);

        Assertions.assertTrue(resource.isRight());
    }

    @Test
    @DisplayName("When type doesn't match any definition in schema then should not be created")
    void typeNotMatchingDefinition() {
        validJson.put("@type", "ABC");

        var resource = resourceService.createResource(validJson, false);

        Assertions.assertTrue(resource.isLeft());
    }

    @Test
    @DisplayName("When type of property is wrong resource should not be created")
    void wrongType() {
        var resource = resourceService.createResource(corruptedCategoryJson, false);

        Assertions.assertTrue(resource.isLeft());
    }
}