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
        var resolver = new RequestCurrentUserResolver(false, tokenConverterProperties());

        assertEquals(CurrentUserResolver.UNKNOWN_USER, resolver.resolveCurrentUser());
    }

    @Test
    void shouldResolveUserFromToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwtWithUsername("adamski")));

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties());

        assertEquals("adamski", resolver.resolveCurrentUser());
    }

    @Test
    void shouldResolveUserFromHeaderForServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwtWithUsername("service-account-maat")));
        setCurrentRequest("adamski");

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties());

        assertEquals("adamski", resolver.resolveCurrentUser());
    }

    @Test
    void shouldReturnUnknownForServiceTokenWithoutHeader() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwtWithUsername("service-account-maat")));

        var resolver = new RequestCurrentUserResolver(true, tokenConverterProperties());

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

    private static TokenConverterProperties tokenConverterProperties() {
        TokenConverterProperties properties = new TokenConverterProperties();
        properties.setPrincipalAttribute("preferred_username");
        return properties;
    }
}
