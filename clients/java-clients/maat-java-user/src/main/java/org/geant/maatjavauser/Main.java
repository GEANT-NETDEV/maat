package org.geant.maatjavauser;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws Exception {
        Authentication auth = new Authentication(
                "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/auth",
                "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/token",
                "https://1.2.3.4:8082/resourceInventoryManagement/v4.0.0/resource",
                "maat-account",
                "d0b8122f-8dfb-46b7-b68a-f5cc4e25d123",
                "https://1.2.3.4:8082",
                "user",
                "password"
        );

        auth.getToken();
    }
}