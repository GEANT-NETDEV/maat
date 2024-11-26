package org.geant.maatjavaclient;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        Authentication auth = new Authentication(
                "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/token",
                "https://1.2.3.4:8082/resourceInventoryManagement/v4.0.0/resource",
                "maat",
                "d0b8122f-8dfb-46b7-b68a-f5cc4e25d123");
        auth.getToken();
    }
}