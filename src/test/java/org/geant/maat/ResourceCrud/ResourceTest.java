package org.geant.maat.ResourceCrud;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geant.maat.resource.Resource;
import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import org.geant.maat.utils.ResourceReader;
import org.junit.jupiter.api.BeforeEach;

class ResourceTest{
    ResourceService resourceService;
    ObjectNode validJson;
    ObjectNode corruptedCategoryJson;
    Resource resource123;
    Resource resource456;

    @BeforeEach
    void init() {
        resourceService = new ResourceConfiguration().inMemoryResourceService();
        validJson = ResourceReader.getDefaultResourceWithRandomId();
        corruptedCategoryJson = validJson.deepCopy().put("category", 123);
        resource123 = new Resource(resourceService.createResource(validJson.put("name", "123"), false).get());
        resource456 = new Resource(resourceService.createResource(validJson.put("name", "456"), false).get());
    }
}
