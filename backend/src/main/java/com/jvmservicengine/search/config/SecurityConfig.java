package com.jvmservicengine.search.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Value("${admin.api-key:#{null}}")
    private String adminApiKey;

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateApiKey() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (adminApiKey == null || adminApiKey.isBlank()) {
            if (isDev) {
                log.warn("admin.api-key / ADMIN_API_KEY is not set. Protected endpoints are OPEN in dev mode.");
            } else {
                throw new IllegalStateException(
                        "admin.api-key (env ADMIN_API_KEY) must be set in non-dev profiles. " +
                        "Set the environment variable ADMIN_API_KEY before starting the application.");
            }
        }
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public: search only
                .requestMatchers(HttpMethod.GET, "/api/v1/search").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        // Add our filter before the standard username/password filter.
        // If adminApiKey is null/blank in dev mode we skip adding it (all requests pass through).
        if (adminApiKey != null && !adminApiKey.isBlank()) {
            http.addFilterBefore(new ApiKeyAuthFilter(adminApiKey),
                    UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-API-Key"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

