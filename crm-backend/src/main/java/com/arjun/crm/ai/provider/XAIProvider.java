package com.arjun.crm.ai.provider;

import com.arjun.crm.ai.dto.response.AIResponse;
import com.arjun.crm.exception.AIServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI provider for any OpenAI-compatible chat completions API
 * (Groq by default; xAI or OpenAI work by changing ai.xai.base-url and model).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class XAIProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.xai.api-key}")
    private String apiKey;

    @Value("${ai.xai.model}")
    private String model;

    @Value("${ai.xai.base-url}")
    private String baseUrl;

    @Value("${ai.xai.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${ai.xai.retry.backoff-delay:1000}")
    private long backoffDelay;

    /**
     * Generate AI response (delegates to generateResponseNoCache — caching is
     * handled at the service layer, not here, to avoid Redis type-cast issues).
     */
    public AIResponse generateResponse(String prompt) {
        return generateResponseNoCache(prompt);
    }

    /**
     * Generate AI response without caching (for dynamic content).
     * Retries rate limits (429), server errors (5xx) and network errors with
     * exponential backoff, then falls back to a "temporarily unavailable" response.
     */
    public AIResponse generateResponseNoCache(String prompt) {
        long delay = backoffDelay;
        for (int attempt = 1; ; attempt++) {
            try {
                return callApi(prompt);
            } catch (HttpStatusCodeException e) {
                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                if (!retryable || attempt >= maxAttempts) {
                    log.error("AI API call failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return unavailable();
                }
                log.warn("AI API returned {} (attempt {}/{}), retrying in {} ms", e.getStatusCode(), attempt, maxAttempts, delay);
            } catch (ResourceAccessException e) {
                if (attempt >= maxAttempts) {
                    log.error("AI API unreachable: {}", e.getMessage());
                    return unavailable();
                }
                log.warn("AI API unreachable (attempt {}/{}), retrying in {} ms", attempt, maxAttempts, delay);
            } catch (Exception e) {
                log.error("Error calling AI API: {}", e.getMessage(), e);
                return unavailable();
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return unavailable();
            }
            delay *= 2;
        }
    }

    private AIResponse callApi(String prompt) {
        log.info("Sending request to AI API (model={})", model);

        Map<String, Object> requestBody = buildRequestBody(prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return parseXAIResponse(response.getBody());
        } else {
            throw new AIServiceException("Failed to get response from AI API: " + response.getStatusCode());
        }
    }

    private AIResponse unavailable() {
        // Graceful fallback
        return AIResponse.builder()
            .content("AI service is temporarily unavailable. Please try again shortly.")
            .success(false)
            .model(model)
            .build();
    }

    /**
     * Build xAI/Groq API request body using the OpenAI-compatible /v1/chat/completions format.
     * A system message enforces pure JSON output so the parser never gets prose.
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        // System message: force strict JSON-only output
        messages.add(Map.of(
            "role", "system",
            "content", "You are a helpful AI assistant. IMPORTANT: Always respond with ONLY valid JSON. No prose, no explanations, no markdown fences. Output the raw JSON object directly."
        ));
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        return requestBody;
    }

    /**
     * Parse xAI API response (OpenAI-compatible format)
     */
    private AIResponse parseXAIResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            String content = null;
            
            // Try OpenAI-compatible format (choices[0].message.content)
            if (root.has("choices")) {
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    JsonNode message = firstChoice.path("message");
                    content = message.path("content").asText();
                }
            }
            // Fallbacks for other formats
            else if (root.has("response")) {
                content = root.path("response").asText();
            }
            else if (root.has("output")) {
                content = root.path("output").asText();
            }
            else if (root.has("text")) {
                content = root.path("text").asText();
            }
            
            if (content != null && !content.trim().isEmpty()) {
                return AIResponse.builder()
                    .content(content.trim())
                    .success(true)
                    .model(model)
                    .build();
            }
            
            // Log the actual response for debugging
            log.warn("Unexpected xAI response format: {}", responseBody);
            
            // Fallback if no valid content found
            return AIResponse.builder()
                .content("AI service returned an empty response. Please try again.")
                .success(false)
                .model(model)
                .build();
            
        } catch (Exception e) {
            log.error("Error parsing xAI response: {}", e.getMessage(), e);
            log.error("Response body was: {}", responseBody);
            return AIResponse.builder()
                .content("AI service is temporarily unavailable. Please try again shortly.")
                .success(false)
                .model(model)
                .build();
        }
    }
}