package com.growthpilot.ai;

public interface AiProvider {
    String generateText(String systemPrompt, String userPrompt);
    boolean isAvailable();
    String getProviderName();
}
