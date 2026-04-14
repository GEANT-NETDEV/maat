package org.geant.maat.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "keycloak.service-token-user-context.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KeycloakServiceTokenUserContextConfiguration {
    private static void validateProperties(KeycloakServiceTokenUserContextProperties properties) {
        if (properties.getTrustedClient() == null || properties.getTrustedClient().isBlank()) {
            throw new IllegalStateException("keycloak.service-token-user-context.trusted-client must be configured when the mechanism is enabled");
        }
        if (properties.getAdminClientId() == null || properties.getAdminClientId().isBlank()) {
            throw new IllegalStateException("keycloak.service-token-user-context.admin-client-id must be configured when the mechanism is enabled");
        }
        if (properties.getAdminClientSecret() == null || properties.getAdminClientSecret().isBlank()) {
            throw new IllegalStateException("keycloak.service-token-user-context.admin-client-secret must be configured when the mechanism is enabled");
        }
    }

    @Bean
    KeycloakAdminTokenService keycloakAdminTokenService(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            KeycloakServiceTokenUserContextProperties properties) {
        validateProperties(properties);
        return new KeycloakAdminTokenService(issuerUri, properties);
    }

    @Bean
    KeycloakAdminClient keycloakAdminClient(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            TokenConverterProperties tokenConverterProperties,
            KeycloakAdminTokenService adminTokenService) {
        return new KeycloakAdminClient(issuerUri, tokenConverterProperties.getResourceId(), adminTokenService);
    }

    @Bean
    KeycloakServiceTokenUserContextService keycloakServiceTokenUserContextService(
            KeycloakAdminClient keycloakAdminClient,
            KeycloakServiceTokenUserContextProperties properties,
            ObjectMapper objectMapper) {
        return new KeycloakServiceTokenUserContextService(keycloakAdminClient, properties, objectMapper);
    }

    @Bean
    EffectiveUserJwtFactory effectiveUserJwtFactory(TokenConverterProperties tokenConverterProperties) {
        return new EffectiveUserJwtFactory(tokenConverterProperties);
    }

    @Bean
    KeycloakServiceTokenUserContextFilter keycloakServiceTokenUserContextFilter(
            KeycloakServiceTokenUserContextProperties properties,
            KeycloakServiceTokenUserContextService userContextService,
            EffectiveUserJwtFactory effectiveUserJwtFactory,
            KeycloakJwtTokenConverter keycloakJwtTokenConverter,
            ObjectMapper objectMapper) {
        return new KeycloakServiceTokenUserContextFilter(
                properties,
                userContextService,
                effectiveUserJwtFactory,
                keycloakJwtTokenConverter,
                objectMapper
        );
    }
}
