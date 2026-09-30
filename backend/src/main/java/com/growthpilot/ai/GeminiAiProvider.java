package com.growthpilot.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    @Value("${ai.provider.api-key:}")
    private String apiKey;

    @Value("${ai.provider.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.provider.endpoint:https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent}")
    private String endpoint;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("YOUR_");
    }

    @Override
    public String getProviderName() {
        return "Google Gemini (" + model + ")";
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        try {
            String url = endpoint.replace("{model}", model) + "?key=" + apiKey;
            String promptPayload = (systemPrompt != null && !systemPrompt.isBlank())
                    ? systemPrompt + "\n\nUser Question:\n" + userPrompt
                    : userPrompt;

            String escaped = promptPayload
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String body = """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [{"text": "%s"}]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.3,
                    "maxOutputTokens": 2048
                  }
                }
                """.formatted(escaped);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseGeminiResponse(response.body());
            } else {
                log.warn("Gemini API returned error status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API error: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to query Gemini AI provider: {}", e.getMessage());
            throw new RuntimeException("AI Provider failure: " + e.getMessage(), e);
        }
    }

    private String parseGeminiResponse(String json) {
        try {
            int textIdx = json.indexOf("\"text\": \"");
            if (textIdx != -1) {
                int start = textIdx + 9;
                int end = json.indexOf("\"", start);
                while (end > 0 && json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }
                if (end > start) {
                    return json.substring(start, end)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                }
            }
            return json;
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON: {}", e.getMessage());
            return "Unable to parse AI response.";
        }
    }
}
