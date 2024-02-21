package org.geant.maat.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class ResourceReader extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    public static String getLogicalResource() {
        return getStringResource("resource.json", "schema.json");
    }

    public static ObjectNode getResource(String resourceName, String schemaName) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode on;
        try {
            on = (ObjectNode) mapper.readTree(getResourceUrl(resourceName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        on.put("@schemaLocation", getResourcePath(schemaName));
        return on;
    }

    public static String getStringResource(String resourceName, String schemaName) {
        return getResource(resourceName, schemaName).toString();
    }

    public static URL getResourceUrl(String name) {
        return ResourceReader.class.getClassLoader().getResource((name));
    }

    public static String getResourcePath(String name) {
        return getResourceUrl(name).toString();
    }

    public static ObjectNode getDefaultResource() {
        return getResource("resource.json", "schema.json");
    }

    public static ObjectNode getDefaultResourceWithRandomId() {
        var on = (ObjectNode) getResource("resource.json", "schema.json");
        on.put("id", UUID.randomUUID().toString());
        return on;
    }
}
