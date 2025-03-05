package org.geant.maat.common;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
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
                            .requestMatchers(HttpMethod.OPTIONS).permitAll()
                            .requestMatchers(HttpMethod.GET).hasRole(getOnlyRole)
                            .requestMatchers(HttpMethod.POST).hasRole(postOnlyRole)
                            .requestMatchers(HttpMethod.DELETE).hasRole(deleteOnlyRole)
                            .requestMatchers(HttpMethod.PATCH).hasRole(patchOnlyRole)
                            .anyRequest()
                            .authenticated());
        } else {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(HttpMethod.OPTIONS).permitAll()
                            .anyRequest()
                            .authenticated());
        }

        http
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtTokenConverter)));
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();

                String origin = request.getHeader("Origin");

                if (origin != null) {
                    config.setAllowedOrigins(List.of(origin));
                }

                config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of(
                        "Content-Type",
                        "Origin",
                        "Accept",
                        "X-Requested-With",
                        "remember-me",
                        "Authorization"
                ));
                config.setAllowCredentials(true);

                return config;
            }
        };
    }
}

