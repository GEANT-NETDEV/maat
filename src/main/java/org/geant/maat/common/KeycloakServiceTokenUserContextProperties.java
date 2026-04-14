package org.geant.maat.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "keycloak.service-token-user-context")
public class KeycloakServiceTokenUserContextProperties {
    private boolean enabled = false;
    private String headerName = CurrentUserResolver.AUTHENTICATED_USER_HEADER;
    private String trustedClient;
    private String adminClientId;
    private String adminClientSecret;
    private long cacheTtlSeconds = 300;
    private boolean failOnKeycloakError = true;
    private List<String> userAccessFilterAttributes = new ArrayList<>(List.of("user_access_filters"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getTrustedClient() {
        return trustedClient;
    }

    public void setTrustedClient(String trustedClient) {
        this.trustedClient = trustedClient;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public boolean isFailOnKeycloakError() {
        return failOnKeycloakError;
    }

    public void setFailOnKeycloakError(boolean failOnKeycloakError) {
        this.failOnKeycloakError = failOnKeycloakError;
    }

    public List<String> getUserAccessFilterAttributes() {
        return userAccessFilterAttributes;
    }

    public void setUserAccessFilterAttributes(List<String> userAccessFilterAttributes) {
        this.userAccessFilterAttributes = userAccessFilterAttributes;
    }
}
