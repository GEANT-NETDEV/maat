package org.geant.maat.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakSecurityConfig {
    @Value("${spring.security.role.get_only}")
    private String getOnlyRole;
    @Value("${spring.security.role.post_only}")
    private String postOnlyRole;
    @Value("${spring.security.role.delete_only}")
    private String deleteOnlyRole;
    @Value("${spring.security.role.patch_only}")
    private String patchOnlyRole;

    private final KeycloakJwtTokenConverter keycloakJwtTokenConverter;


    public KeycloakSecurityConfig(TokenConverterProperties properties) {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter
                = new JwtGrantedAuthoritiesConverter();
        this.keycloakJwtTokenConverter
                = new KeycloakJwtTokenConverter(
                jwtGrantedAuthoritiesConverter,
                properties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET).hasRole(getOnlyRole)
                        .requestMatchers(HttpMethod.POST).hasRole(postOnlyRole)
                        .requestMatchers(HttpMethod.DELETE).hasRole(deleteOnlyRole)
                        .requestMatchers(HttpMethod.PATCH).hasRole(patchOnlyRole)
                        .anyRequest()
                        .authenticated());
        http
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtTokenConverter)));
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

