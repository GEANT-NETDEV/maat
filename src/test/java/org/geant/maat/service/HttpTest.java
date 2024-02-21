package org.geant.maat.service;

import org.geant.maat.MaatApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
@DirtiesContext
@SpringBootTest(classes = {MaatApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"security.require-ssl=false", "server.address=localhost", "server.port=12345"})
public class HttpTest extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    @Value("${server.port}")
    public int randomServerPort;

    String serviceUrl() {
        return String.format("http://localhost:%s/serviceInventoryManagement/v4.0.0/service/", randomServerPort);
    }

    String serviceUrlNoSlash() {
        return String.format("http://localhost:%s/serviceInventoryManagement/v4.0.0/service", randomServerPort);
    }
}
