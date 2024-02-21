package org.geant.maat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;



@Component
class ServiceHrefBuilder {

    private boolean https;
    private String protocol;
    private String address;
    private int port;
    private String apiVersion;

    public ServiceHrefBuilder(
            @Value("${server.ssl.enabled:false}") boolean https,
            @Value("${service.protocol:${server.protocol:http}}") String protocol,
            @Value("${service.address:${server.address:localhost}}") String address,
            @Value("${service.port:${server.port:8080}}") int port,
            @Value("${api.service.version:'v4.0.0'}") String apiVersion
    ) {
        this.https = https;
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.apiVersion = apiVersion;
    }

    public static ServiceHrefBuilder builder() {
        return new ServiceHrefBuilder(false, "http", "localhost", 8080, "v4.0.0");
    }

    public ServiceHrefBuilder http(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public ServiceHrefBuilder port(int port) {
        this.port = port;
        return this;
    }

    public ServiceHrefBuilder http() {
        this.https = false;
        return this;
    }

    public ServiceHrefBuilder https() {
        this.https = true;
        return this;
    }

    public ServiceHrefBuilder address(String address) {
        this.address = address;
        return this;
    }

    public ServiceHrefBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public String id(String id) {
        if (https | protocol.equals("https")) {
            return "https" + "://" + address + ":" + port + "/serviceInventoryManagement/" + apiVersion + "/service/" + id;
        } else {
            return "http" + "://" + address + ":" + port + "/serviceInventoryManagement/" + apiVersion + "/service/" + id;
        }
    }

}
