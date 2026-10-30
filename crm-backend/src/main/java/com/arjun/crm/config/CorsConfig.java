package com.arjun.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration
 * Configures Cross-Origin Resource Sharing for the application
 *
 * Uses setAllowedOriginPatterns() to support wildcard patterns (e.g. *.vercel.app)
 * while still allowing credentials. Origins come only from cors.allowed-origins.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5173}")
    private String allowedOriginsRaw;

    /**
     * CORS Configuration Source
     * Reads allowed origins from application config (cors.allowed-origins).
     * Supports wildcard patterns via setAllowedOriginPatterns().
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins/patterns from config
        List<String> configOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Only configured origins are allowed (Spring handles both plain URLs and patterns).
        // Add e.g. https://*-yourteam.vercel.app to CORS_ALLOWED_ORIGINS for preview deployments.
        configuration.setAllowedOriginPatterns(configOrigins);

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.addAllowedHeader("*");

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
