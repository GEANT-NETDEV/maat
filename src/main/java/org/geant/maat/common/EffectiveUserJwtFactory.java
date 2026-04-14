package org.geant.maat.common;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EffectiveUserJwtFactory {
    private final TokenConverterProperties tokenConverterProperties;

    EffectiveUserJwtFactory(TokenConverterProperties tokenConverterProperties) {
        this.tokenConverterProperties = tokenConverterProperties;
    }

    Jwt create(Jwt originalJwt, KeycloakUserContext userContext) {
        Map<String, Object> claims = new HashMap<>(originalJwt.getClaims());
        claims.put("preferred_username", userContext.username());
        claims.put("user_access_filters", userContext.userAccessFilters());
        claims.put("service_token_user_context", true);

        Map<String, Object> originalResourceAccess = originalJwt.getClaimAsMap("resource_access");
        Map<String, Object> resourceAccess = originalResourceAccess == null
                ? new HashMap<>()
                : new HashMap<>(originalResourceAccess);
        Map<String, Object> targetClientAccess = new HashMap<>();
        targetClientAccess.put("roles", new ArrayList<>(userContext.maatClientRoles()));
        resourceAccess.put(tokenConverterProperties.getResourceId(), targetClientAccess);
        claims.put("resource_access", resourceAccess);

        List<String> scopeValues = new ArrayList<>();
        String scope = originalJwt.getClaimAsString("scope");
        if (scope != null && !scope.isBlank()) {
            scopeValues.addAll(List.of(scope.split(" ")));
        }
        if (!scopeValues.contains("user_access_filters")) {
            scopeValues.add("user_access_filters");
        }
        claims.put("scope", String.join(" ", scopeValues));

        return new Jwt(
                originalJwt.getTokenValue(),
                originalJwt.getIssuedAt(),
                originalJwt.getExpiresAt(),
                originalJwt.getHeaders(),
                claims
        );
    }
}
