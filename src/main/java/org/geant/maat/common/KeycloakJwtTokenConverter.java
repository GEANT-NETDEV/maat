package org.geant.maat.common;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class KeycloakJwtTokenConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
    private final TokenConverterProperties properties;

    public KeycloakJwtTokenConverter(
            JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
            TokenConverterProperties properties) {
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
        this.properties = properties;
    }

    @Override
    public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
        List<Collection<String>> roleResources = Optional.of(jwt)
                .map(token -> token.getClaimAsMap(RESOURCE_ACCESS))
                .map(claimMap -> (Map<String, Object>) claimMap.get(properties.getResourceId()))
                .map(resourceData -> (Collection<String>) resourceData.get(ROLES))
                .stream().collect(Collectors.toList());

        List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        roleResources.forEach(role -> role.forEach(roleValue -> simpleGrantedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + roleValue))));

        Stream<SimpleGrantedAuthority> accesses = simpleGrantedAuthorities.stream().distinct();

        Set<GrantedAuthority> authorities = Stream
                .concat(jwtGrantedAuthoritiesConverter.convert(jwt).stream(), accesses)
                .collect(Collectors.toSet());

        String principalClaimName = properties.getPrincipalAttribute()
                .map(jwt::getClaimAsString)
                .orElse(jwt.getClaimAsString(JwtClaimNames.SUB));

        return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
    }

}
