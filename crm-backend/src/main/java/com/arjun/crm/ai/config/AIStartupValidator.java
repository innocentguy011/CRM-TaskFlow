package com.arjun.crm.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates AI configuration at startup and logs a clear warning if
 * the API key is missing, rather than letting it silently fail at
 * runtime when a user actually hits an AI endpoint.
 */
@Component
@Slf4j
public class AIStartupValidator {

    @Value("${ai.xai.api-key:}")
    private String apiKey;

    @Value("${ai.xai.base-url}")
    private String baseUrl;

    @Value("${ai.xai.model}")
    private String model;

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("╔══════════════════════════════════════════════════════════╗");
            log.error("║  AI CONFIGURATION ERROR                                  ║");
            log.error("║  GROQ_API_KEY environment variable is not set.           ║");
            log.error("║  AI features (chat AI, task AI, insights) will fail.     ║");
            log.error("║  Set GROQ_API_KEY in your environment or .env file.      ║");
            log.error("╚══════════════════════════════════════════════════════════╝");
        } else {
            // Log only the key length to confirm it loaded without exposing any of it
            log.info("AI provider configured: model={}, url={}, key length={}", model, baseUrl, apiKey.length());
        }
    }
}
