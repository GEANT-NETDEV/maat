package org.geant.maat.ServiceCrud;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geant.maat.service.ServiceConfiguration;
import org.geant.maat.service.ServiceService;
import org.geant.maat.service.Service;
import org.geant.maat.utils.ServiceReader;
import org.junit.jupiter.api.BeforeEach;

public class ServiceTest{
    ServiceService serviceService;
    ObjectNode validJson;
    ObjectNode corruptedCategoryJson;
    Service service123;
    Service service456;

    @BeforeEach
    void init() {
        serviceService = new ServiceConfiguration().inMemoryServiceService();
        validJson = ServiceReader.getDefaultServiceWithRandomId();
        corruptedCategoryJson = validJson.deepCopy().put("category", 123);
        service123 = new Service(serviceService.createService(validJson.put("name", "123"), false).get());
        service456 = new Service(serviceService.createService(validJson.put("name", "456"), false).get());
    }

}
