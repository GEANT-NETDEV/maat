package org.geant.maat.resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ResourceHrefBuilder {
    private boolean https;
    private String protocol;
    private String address;
    private int port;
    private String apiVersion;

    public ResourceHrefBuilder(
            @Value("${server.ssl.enabled:false}") boolean https,
            @Value("${resource.protocol:${server.protocol:http}}") String protocol,
            @Value("${resource.address:${server.address:localhost}}") String address,
            @Value("${resource.port:${server.port:8080}}") int port,
            @Value("${api.resource.version:'v4.0.0'}") String apiVersion
    ) {
        this.https = https;
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.apiVersion = apiVersion;
    }

    public static ResourceHrefBuilder builder() {
        return new ResourceHrefBuilder(false, "http", "localhost", 8080, "v4.0.0");
    }

    public ResourceHrefBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public ResourceHrefBuilder port(int port) {
        this.port = port;
        return this;
    }

    public ResourceHrefBuilder http() {
        this.https = false;
        return this;
    }

    public ResourceHrefBuilder https() {
        this.https = true;
        return this;
    }

    public ResourceHrefBuilder address(String address) {
        this.address = address;
        return this;
    }

    public ResourceHrefBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public String id(String id) {
        if (https | protocol.equals("https")) {
            return "https" + "://" + address + ":" + port + "/resourceInventoryManagement/" + apiVersion + "/resource/" + id;
        } else {
            return "http" + "://" + address + ":" + port + "/resourceInventoryManagement/" + apiVersion + "/resource/" + id;
        }
    }
}
