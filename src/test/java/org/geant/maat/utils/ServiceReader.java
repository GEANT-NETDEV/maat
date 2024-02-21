package org.geant.maat.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class ServiceReader extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    public static String getLogicalService() {
        return getStringService("resource.json", "schema.json");
    }

    public static ObjectNode getService(String serviceName, String schemaName) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode on;
        try {
            on = (ObjectNode) mapper.readTree(getServiceUrl(serviceName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        on.put("@schemaLocation", getServicePath(schemaName));
        return on;
    }

    public static String getStringService(String serviceName, String schemaName) {
        return getService(serviceName, schemaName).toString();
    }

    public static URL getServiceUrl(String name) {
        return ServiceReader.class.getClassLoader().getResource((name));
    }

    public static String getServicePath(String name) {
        return getServiceUrl(name).toString();
    }

    public static ObjectNode getDefaultService() {
        return getService("service.json", "schema-service.json");
    }

    public static ObjectNode getDefaultServiceWithRandomId() {
        var on = (ObjectNode) getService("service.json", "schema-service.json");
        on.put("id", UUID.randomUUID().toString());
        return on;
    }


}
