package org.geant.maat.ServiceCrud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When creating resource from json")
public class CreateTest extends ServiceTest{

    @Test
    @DisplayName("mandatory properties - 'id', 'href' and serviceDate should be added")
    void shouldContainMandatory() {
        var service = serviceService.createService(validJson, false).get();

        assertTrue(service.has("id"));
        assertTrue(service.has("href"));
        assertTrue(service.has("serviceDate"));
    }

    @Test
    @DisplayName("When @type missing should create service without validation")
    void typeMissing() {
        corruptedCategoryJson.remove("@type");

        var newService = serviceService.createService(validJson, false);

        assertTrue(newService.isRight());
    }

    @Test
    @DisplayName("When @schemaLocation missing should create service without validation")
    void schemaLocationMissing() {
        corruptedCategoryJson.remove("@schemaLocation");

        var newService = serviceService.createService(validJson, false);

        assertTrue(newService.isRight());
    }

    @Test
    @DisplayName("When @type and @schemaLocation are present and json matches the schema then it should be created")
    void goodService() {
        var service = serviceService.createService(validJson, false);

        Assertions.assertTrue(service.isRight());
    }

    @Test
    @DisplayName("When type doesn't match any definition in schema then should not be created")
    void typeNotMatchingDefinition() {
        validJson.put("@type", "ABC");

        var service = serviceService.createService(validJson, false);

        Assertions.assertTrue(service.isLeft());
    }

    @Test
    @DisplayName("When type of property is wrong service should not be created")
    void wrongType() {
        var service = serviceService.createService(corruptedCategoryJson, false);

        Assertions.assertTrue(service.isLeft());
    }

}
