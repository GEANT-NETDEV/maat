package org.geant.maat.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakServiceTokenUserContextFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReplaceSecurityContextForTrustedServiceToken() throws ServletException, IOException {
        KeycloakServiceTokenUserContextProperties properties = new KeycloakServiceTokenUserContextProperties();
        properties.setTrustedClient("maat");

        TokenConverterProperties tokenConverterProperties = new TokenConverterProperties();
        tokenConverterProperties.setResourceId("maat-account");
        tokenConverterProperties.setPrincipalAttribute("preferred_username");

        KeycloakServiceTokenUserContextService userContextService = mock(KeycloakServiceTokenUserContextService.class);
        when(userContextService.load("test")).thenReturn(new KeycloakUserContext(
                "test",
                Map.of("get_filter", List.of(Map.of("category", "device.router"))),
                List.of("get")
        ));

        KeycloakServiceTokenUserContextFilter filter = new KeycloakServiceTokenUserContextFilter(
                properties,
                userContextService,
                new EffectiveUserJwtFactory(tokenConverterProperties),
                new KeycloakJwtTokenConverter(new JwtGrantedAuthoritiesConverter(), tokenConverterProperties),
                objectMapper
        );

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(serviceJwt("maat")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getHeaderName(), "test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertEquals("test", authentication.getToken().getClaimAsString("preferred_username"));
        assertTrue(authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_get")));
    }

    @Test
    void shouldRejectUntrustedClient() throws ServletException, IOException {
        KeycloakServiceTokenUserContextProperties properties = new KeycloakServiceTokenUserContextProperties();
        properties.setTrustedClient("maat");

        TokenConverterProperties tokenConverterProperties = new TokenConverterProperties();
        tokenConverterProperties.setResourceId("maat-account");
        tokenConverterProperties.setPrincipalAttribute("preferred_username");

        KeycloakServiceTokenUserContextFilter filter = new KeycloakServiceTokenUserContextFilter(
                properties,
                mock(KeycloakServiceTokenUserContextService.class),
                new EffectiveUserJwtFactory(tokenConverterProperties),
                new KeycloakJwtTokenConverter(new JwtGrantedAuthoritiesConverter(), tokenConverterProperties),
                objectMapper
        );

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(serviceJwt("other-client")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getHeaderName(), "test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    private Jwt serviceJwt(String azp) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", "service-account-maat")
                .claim("azp", azp)
                .claim("client_id", azp)
                .claim("resource_access", Map.of("account", Map.of("roles", List.of("view-profile"))))
                .build();
    }
}
