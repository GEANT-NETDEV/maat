package org.geant.maat.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceHrefBuilderTest {
    private ResourceHrefBuilder builder;

    @BeforeEach
    void init() {
        this.builder = ResourceHrefBuilder.builder();
    }

    @Test
    @DisplayName("default href should be http://localhost:8080/resourceInventoryManagement/v4.0.0/resource/")
    void defaultHref() {
        assertEquals("http://localhost:8080/resourceInventoryManagement/v4.0.0/resource/", this.builder.id(""));
    }

    @Test
    @DisplayName("should be able to set address port and https")
    void setAll() {
        var href = builder.address("1.2.3.4").https().port(9876).id("id");
        assertEquals("https://1.2.3.4:9876/resourceInventoryManagement/v4.0.0/resource/id", href);
    }
}