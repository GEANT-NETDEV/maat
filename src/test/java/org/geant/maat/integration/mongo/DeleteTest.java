package org.geant.maat.integration.mongo;

import org.geant.maat.resource.Resource;
import org.geant.maat.resource.ResourceConfiguration;
import org.geant.maat.resource.ResourceService;
import org.geant.maat.utils.ResourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Testcontainers
class DeleteTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    private ResourceService resourceService;
    private Resource createdResource;

    @BeforeEach
    void init() {
        resourceService = new ResourceConfiguration().resourceServiceWithTestMongo();
        createdResource = Resource.from(
                resourceService.createResource(ResourceReader.getDefaultResource(), false).get().toString());
    }

    @Test
    @DisplayName("It should be able to delete resource by id")
    void byId() {
        var result = resourceService.deleteResource(createdResource.getId(), false);
        assertTrue(result.isRight());
    }

    @Test
    @DisplayName("When deleting resource with wrong id should get error")
    void wrongId() {
        var result = resourceService.deleteResource("wrong id", false);
        assertTrue(result.isLeft());
    }
}