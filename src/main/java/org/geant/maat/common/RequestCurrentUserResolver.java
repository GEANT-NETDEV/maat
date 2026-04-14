package org.geant.maat.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class RequestCurrentUserResolver implements CurrentUserResolver {
    private final boolean keycloakEnabled;
    private final String principalClaimName;
    private final KeycloakServiceTokenUserContextProperties properties;

    public RequestCurrentUserResolver(
            @Value("${keycloak.enabled}") boolean keycloakEnabled,
            TokenConverterProperties tokenConverterProperties,
            KeycloakServiceTokenUserContextProperties properties) {
        this.keycloakEnabled = keycloakEnabled;
        this.principalClaimName = tokenConverterProperties.getPrincipalAttribute().orElse("preferred_username");
        this.properties = properties;
    }

    @Override
    public String resolveCurrentUser() {
        var currentRequest = currentRequest();
        if (currentRequest.isPresent()) {
            Object cachedUser = currentRequest.get().getAttribute(CURRENT_USER_REQUEST_ATTRIBUTE);
            if (cachedUser instanceof String user && StringUtils.hasText(user)) {
                return user;
            }
        }

        String user = resolveUserInternal(currentRequest.orElse(null));
        currentRequest.ifPresent(request -> request.setAttribute(CURRENT_USER_REQUEST_ATTRIBUTE, user));
        return user;
    }

    private String resolveUserInternal(HttpServletRequest request) {
        if (!keycloakEnabled) {
            return UNKNOWN_USER;
        }

        Optional<String> tokenUser = resolveFromJwt();
        if (tokenUser.isPresent() && !isServiceAccount(tokenUser.get())) {
            return tokenUser.get();
        }

        if (request != null) {
            String headerUser = request.getHeader(properties.getHeaderName());
            if (StringUtils.hasText(headerUser) && isHeaderTrustedForCurrentToken()) {
                return headerUser.trim();
            }
        }

        return UNKNOWN_USER;
    }

    private Optional<String> resolveFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return claimValue(jwtAuthenticationToken.getToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return claimValue(jwt);
        }
        return Optional.empty();
    }

    private Optional<String> claimValue(Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsString(principalClaimName))
                .filter(StringUtils::hasText)
                .map(String::trim);
    }

    private boolean isHeaderTrustedForCurrentToken() {
        if (!properties.isEnabled()) {
            return true;
        }
        return getCurrentJwt()
                .map(jwt -> {
                    String trustedClient = properties.getTrustedClient();
                    String azp = jwt.getClaimAsString("azp");
                    String clientId = jwt.getClaimAsString("client_id");
                    return StringUtils.hasText(trustedClient)
                            && (trustedClient.equals(azp) || trustedClient.equals(clientId));
                })
                .orElse(false);
    }

    private boolean isServiceAccount(String user) {
        return user.startsWith("service-account-");
    }

    private Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return Optional.of(jwtAuthenticationToken.getToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }
}
