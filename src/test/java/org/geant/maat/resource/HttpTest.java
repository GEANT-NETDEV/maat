package org.geant.maat.resource;


import org.geant.maat.MaatApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {MaatApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"security.require-ssl=false", "server.address=localhost", "server.port=12345"})
public class HttpTest extends org.geant.maat.integration.testcontainers.BaseTestContainers {
    @Value("${server.port}")
    public int randomServerPort;

    String resourceUrl() {
        return String.format("http://localhost:%s/resourceInventoryManagement/v4.0.0/resource/", randomServerPort);
    }

    public String notificationUrl() {
        return String.format("http://localhost:%s/hub", randomServerPort);
    }

    String resourceUrlNoSlash() {
        return String.format("http://localhost:%s/resourceInventoryManagement/v4.0.0/resource", randomServerPort);
    }
}
