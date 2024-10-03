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

import java.util.Objects;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakSecurityConfig {
    @Value("${maat.role.get_only}")
    private String getOnlyRole;
    @Value("${maat.role.post_only}")
    private String postOnlyRole;
    @Value("${maat.role.delete_only}")
    private String deleteOnlyRole;
    @Value("${maat.role.patch_only}")
    private String patchOnlyRole;
    @Value("${keycloak.authorization.l1.roles}")
    private String keycloakAuthorizationL1Status;

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
        if (Objects.equals(keycloakAuthorizationL1Status, "true")) {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(HttpMethod.GET).hasRole(getOnlyRole)
                            .requestMatchers(HttpMethod.POST).hasRole(postOnlyRole)
                            .requestMatchers(HttpMethod.DELETE).hasRole(deleteOnlyRole)
                            .requestMatchers(HttpMethod.PATCH).hasRole(patchOnlyRole)
                            .anyRequest()
                            .authenticated());
        } else {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest()
                            .authenticated());
        }

        http
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtTokenConverter)));
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

