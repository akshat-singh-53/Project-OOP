package com.itsm.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsm.domain.catalog.Category;
import com.itsm.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter to any OpenAI-compatible chat-completions API (OpenAI, Groq, Gemini's OpenAI endpoint,
 * a local Ollama server...). It is only active when an API key is configured; otherwise the
 * system falls back to the built-in keyword and history classifiers.
 */
@Component
public class LlmCategoryClassifier implements CategoryClassifier {

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public LlmCategoryClassifier(ObjectMapper mapper,
                                 @Value("${itsm.ai.api-key:}") String apiKey,
                                 @Value("${itsm.ai.base-url:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${itsm.ai.model:gpt-4o-mini}") String model) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String name() {
        return "llm:" + model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public List<CategorySuggestion> classify(String text, List<Category> categories) {
        if (!isConfigured()) {
            return List.of();
        }
        String codes = categories.stream()
                .map(c -> c.getCode() + " (" + c.getName() + ")")
                .collect(Collectors.joining(", "));
        String prompt = "You are an IT service desk triage assistant. Classify the incident into exactly one "
                + "category code from this list: " + codes + ". Reply with the code only.\n\nIncident: " + text;
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0,
                    "messages", List.of(Map.of("role", "user", "content", prompt))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode());
            }
            JsonNode json = mapper.readTree(response.body());
            String answer = json.path("choices").path(0).path("message").path("content").asText("")
                    .trim().toUpperCase(Locale.ROOT);
            return categories.stream()
                    .filter(c -> answer.contains(c.getCode()))
                    .findFirst()
                    .map(c -> List.of(new CategorySuggestion(c.getId(), c.getCode(), c.getName(), 0.9,
                            List.of("LLM answer: " + answer), name())))
                    .orElse(List.of());
        } catch (IOException ex) {
            throw new ExternalServiceException("AI classification service", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("AI classification service", ex);
        }
    }
}
