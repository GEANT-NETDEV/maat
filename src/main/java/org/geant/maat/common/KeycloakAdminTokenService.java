package org.geant.maat.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

class KeycloakAdminTokenService {
    private final RestClient restClient;
    private final KeycloakServiceTokenUserContextProperties properties;
    private final String tokenEndpoint;

    private volatile String accessToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    KeycloakAdminTokenService(String issuerUri,
                              KeycloakServiceTokenUserContextProperties properties) {
        this.restClient = RestClient.builder().build();
        this.properties = properties;
        this.tokenEndpoint = issuerUri + "/protocol/openid-connect/token";
    }

    synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(10))) {
            return accessToken;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", properties.getAdminClientId());
            form.add("client_secret", properties.getAdminClientSecret());

            JsonNode response = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.get("access_token") == null) {
                throw new KeycloakServiceTokenUserContextException(503, "Keycloak admin token response missing access_token");
            }

            accessToken = response.get("access_token").asText();
            long expiresIn = response.has("expires_in") ? response.get("expires_in").asLong(60) : 60;
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return accessToken;
        } catch (RestClientException exception) {
            throw new KeycloakServiceTokenUserContextException(503,
                    "Could not obtain Keycloak admin access token: " + exception.getMessage());
        }
    }
}
