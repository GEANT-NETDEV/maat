package org.geant.maat.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.geant.maat.infrastructure.ErrorEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

class KeycloakServiceTokenUserContextFilter extends OncePerRequestFilter {
    private final KeycloakServiceTokenUserContextProperties properties;
    private final KeycloakServiceTokenUserContextService userContextService;
    private final EffectiveUserJwtFactory effectiveUserJwtFactory;
    private final KeycloakJwtTokenConverter jwtTokenConverter;
    private final ObjectMapper objectMapper;

    KeycloakServiceTokenUserContextFilter(KeycloakServiceTokenUserContextProperties properties,
                                          KeycloakServiceTokenUserContextService userContextService,
                                          EffectiveUserJwtFactory effectiveUserJwtFactory,
                                          KeycloakJwtTokenConverter jwtTokenConverter,
                                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.userContextService = userContextService;
        this.effectiveUserJwtFactory = effectiveUserJwtFactory;
        this.jwtTokenConverter = jwtTokenConverter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = authentication.getToken();
        if (!isServiceAccountToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        String delegatedUsername = request.getHeader(properties.getHeaderName());
        if (delegatedUsername == null || delegatedUsername.isBlank()) {
            writeError(response, 400, "Missing required header " + properties.getHeaderName());
            return;
        }

        if (!isTrustedClient(jwt)) {
            writeError(response, 403, "Service token client is not allowed to delegate user context");
            return;
        }

        try {
            KeycloakUserContext userContext = userContextService.load(delegatedUsername.trim());
            Jwt effectiveJwt = effectiveUserJwtFactory.create(jwt, userContext);
            JwtAuthenticationToken effectiveAuthentication = jwtTokenConverter.convert(effectiveJwt);
            effectiveAuthentication.setDetails(authentication.getDetails());
            SecurityContextHolder.getContext().setAuthentication(effectiveAuthentication);
            filterChain.doFilter(request, response);
        } catch (KeycloakServiceTokenUserContextException exception) {
            if (exception.getHttpStatus() == 503 && !properties.isFailOnKeycloakError()) {
                filterChain.doFilter(request, response);
                return;
            }
            writeError(response, exception.getHttpStatus(), exception.getMessage());
        }
    }

    private boolean isServiceAccountToken(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null && username.startsWith("service-account-");
    }

    private boolean isTrustedClient(Jwt jwt) {
        String trustedClient = properties.getTrustedClient();
        String azp = jwt.getClaimAsString("azp");
        String clientId = jwt.getClaimAsString("client_id");
        return trustedClient != null && !trustedClient.isBlank()
                && (trustedClient.equals(azp) || trustedClient.equals(clientId));
    }

    private void writeError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorEntity(message)));
    }
}
