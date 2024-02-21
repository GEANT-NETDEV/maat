package org.geant.maat.resource;

import org.geant.maat.utils.ResourceReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class RecursiveValidatorTest {
    private final ResourceValidator validator = new RecursiveResourceValidator();

    // The idea was that validator should validate object and additionally validated every nested object that has
    // @schemaLocation. But since nobody could give reasonable algorithm that detects (and refuse) props that are
    // missing in schema, but allows in the same time for nested props to define their additional props by its own
    // in @schemaLocation. The implementation below unfortunately allows the additional props.
    // You can uncomment @Test and check how it was supposed to work.
    // @Test
    void recursiveParsing() {
        ObjectMapper mapper = new ObjectMapper();
        var resource = ResourceReader.getResource("recursive_validation/resource.json",
                                                  "recursive_validation/schema_a.json");
        ((ObjectNode) resource.get("bbb")).put("@schemaLocation",
                                               ResourceReader.getResourcePath("recursive_validation/schema_b.json"));

        var result = validator.validate(resource);

        if (result.isLeft()) {
            System.out.println("Errors found:");
            System.out.println(result.getLeft());
        } else {
            System.out.println("No errors");
        }
    }
}