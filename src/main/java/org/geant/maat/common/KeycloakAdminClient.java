package org.geant.maat.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

class KeycloakAdminClient {
    private final RestClient restClient;
    private final KeycloakAdminTokenService adminTokenService;
    private final String adminRealmUrl;
    private final String resourceClientId;

    private volatile String resourceClientUuid;

    KeycloakAdminClient(String issuerUri,
                        String resourceClientId,
                        KeycloakAdminTokenService adminTokenService) {
        this.restClient = RestClient.builder().build();
        this.adminTokenService = adminTokenService;
        this.resourceClientId = resourceClientId;
        this.adminRealmUrl = toAdminRealmUrl(issuerUri);
    }

    Optional<JsonNode> findUserByUsername(String username) {
        String uri = UriComponentsBuilder.fromUriString(adminRealmUrl + "/users")
                .queryParam("username", username)
                .queryParam("exact", true)
                .build()
                .toUriString();

        JsonNode users = getJson(uri);
        if (users == null || !users.isArray() || users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    JsonNode getUserDetails(String userId) {
        return getJson(adminRealmUrl + "/users/" + userId);
    }

    JsonNode getUserClientRoles(String userId) {
        return getJson(adminRealmUrl + "/users/" + userId + "/role-mappings/clients/" + getResourceClientUuid() + "/composite");
    }

    private String getResourceClientUuid() {
        if (resourceClientUuid != null) {
            return resourceClientUuid;
        }

        synchronized (this) {
            if (resourceClientUuid != null) {
                return resourceClientUuid;
            }
            String uri = UriComponentsBuilder.fromUriString(adminRealmUrl + "/clients")
                    .queryParam("clientId", resourceClientId)
                    .build()
                    .toUriString();
            JsonNode clients = getJson(uri);
            if (clients == null || !clients.isArray() || clients.isEmpty() || clients.get(0).get("id") == null) {
                throw new KeycloakServiceTokenUserContextException(503,
                        "Could not resolve Keycloak client id for " + resourceClientId);
            }
            resourceClientUuid = clients.get(0).get("id").asText();
            return resourceClientUuid;
        }
    }

    private JsonNode getJson(String uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(adminTokenService.getAccessToken()))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new KeycloakServiceTokenUserContextException(503,
                    "Could not query Keycloak Admin API: " + exception.getMessage());
        }
    }

    private static String toAdminRealmUrl(String issuerUri) {
        int realmsIndex = issuerUri.indexOf("/realms/");
        if (realmsIndex < 0) {
            throw new IllegalArgumentException("Issuer URI does not contain /realms/: " + issuerUri);
        }
        String host = issuerUri.substring(0, realmsIndex);
        String realm = issuerUri.substring(realmsIndex + "/realms/".length());
        return host + "/admin/realms/" + realm;
    }
}
