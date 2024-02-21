package org.geant.maat.ServiceCrud;

import org.geant.maat.service.Service;
import org.geant.maat.service.ServiceConfiguration;
import org.geant.maat.service.ServiceService;
import org.geant.maat.utils.ServiceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeleteTest {
    private ServiceService serviceService;
    private Service createdService;

    @BeforeEach
    void init() {
        serviceService = new ServiceConfiguration().inMemoryServiceService();
        createdService = Service.from(
                serviceService.createService(ServiceReader.getDefaultService(), false).get().toString());
    }

    @Test
    @DisplayName("It should be able to delete service by id")
    void byId() {
        var result = serviceService.deleteService(createdService.getId(), false);
        assertTrue(result.isRight());
    }

    @Test
    @DisplayName("When deleting service with wrong id should get error")
    void wrongId() {
        var result = serviceService.deleteService("wrong id", false);
        assertTrue(result.isLeft());
    }

}
