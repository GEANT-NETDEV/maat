package org.geant.maat.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveUserJwtFactoryTest {
    @Test
    void shouldCreateEffectiveJwtWithUserRolesAndFilters() {
        TokenConverterProperties properties = new TokenConverterProperties();
        properties.setResourceId("maat-account");
        EffectiveUserJwtFactory factory = new EffectiveUserJwtFactory(properties);

        Jwt originalJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", "service-account-maat")
                .claim("resource_access", Map.of("account", Map.of("roles", List.of("view-profile"))))
                .claim("scope", "profile email")
                .build();

        KeycloakUserContext userContext = new KeycloakUserContext(
                "test",
                Map.of("get_filter", List.of(Map.of("category", "device.router"))),
                List.of("get")
        );

        Jwt effectiveJwt = factory.create(originalJwt, userContext);

        assertEquals("test", effectiveJwt.getClaimAsString("preferred_username"));
        assertTrue(effectiveJwt.getClaimAsString("scope").contains("user_access_filters"));
        assertEquals("get", ((List<?>) ((Map<?, ?>) ((Map<?, ?>) effectiveJwt.getClaimAsMap("resource_access")).get("maat-account")).get("roles")).get(0));
        assertTrue(effectiveJwt.getClaims().containsKey("user_access_filters"));
    }
}
