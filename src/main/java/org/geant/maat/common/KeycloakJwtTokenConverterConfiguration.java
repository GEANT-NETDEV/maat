package org.geant.maat.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class KeycloakJwtTokenConverterConfiguration {

    @Bean
    public KeycloakJwtTokenConverter keycloakJwtTokenConverter(TokenConverterProperties properties) {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        return new KeycloakJwtTokenConverter(jwtGrantedAuthoritiesConverter, properties);
    }
}
