package org.geant.maat.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestCurrentUserResolverTest {
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReturnUnknownWhenKeycloakDisabled() {
        var resolver = new RequestCurrentUserResolver(false, tokenConverterProperties(), serviceTokenProperties(false));

        assertEquals(CurrentUserResolver.UNKNOWN_USER, resolver.resolveCurrentUser());
    }

    @Test
    void shouldResolveUserFromToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwtWithUsername("adamski")));

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties(), serviceTokenProperties(false));

        assertEquals("adamski", resolver.resolveCurrentUser());
    }

    @Test
    void shouldResolveUserFromHeaderForServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(serviceJwt("maat")));
        setCurrentRequest("adamski");

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties(), serviceTokenProperties(true));

        assertEquals("adamski", resolver.resolveCurrentUser());
    }

    @Test
    void shouldReturnUnknownForServiceTokenWithoutHeader() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(serviceJwt("maat")));

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties(), serviceTokenProperties(true));

        assertEquals(CurrentUserResolver.UNKNOWN_USER, resolver.resolveCurrentUser());
    }

    @Test
    void shouldReturnUnknownForUntrustedServiceTokenHeaderWhenMechanismEnabled() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(serviceJwt("other-client")));
        setCurrentRequest("adamski");

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties(), serviceTokenProperties(true));

        assertEquals(CurrentUserResolver.UNKNOWN_USER, resolver.resolveCurrentUser());
    }

    private static void setCurrentRequest(String authenticatedUserId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CurrentUserResolver.AUTHENTICATED_USER_HEADER, authenticatedUserId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static Jwt jwtWithUsername(String username) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .build();
    }

    private static Jwt serviceJwt(String clientId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "service-account-maat")
                .claim("azp", clientId)
                .claim("client_id", clientId)
                .build();
    }

    private static TokenConverterProperties tokenConverterProperties() {
        TokenConverterProperties properties = new TokenConverterProperties();
        properties.setPrincipalAttribute("preferred_username");
        return properties;
    }

    private static KeycloakServiceTokenUserContextProperties serviceTokenProperties(boolean enabled) {
        KeycloakServiceTokenUserContextProperties properties = new KeycloakServiceTokenUserContextProperties();
        properties.setEnabled(enabled);
        properties.setTrustedClient("maat");
        return properties;
    }
}
