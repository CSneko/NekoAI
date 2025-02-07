package org.cneko.ai.providers.openai;

import org.cneko.ai.providers.AbstractAIConfig;

public class OpenAIConfig extends AbstractAIConfig {
    private String endpoint = "/v1/chat/completions";
    public OpenAIConfig(String apiKey) {
        super(apiKey, "gpt-4o-mini", 0.7, "api.openai.com", 443);
    }
    public String getEndpoint() {
        return endpoint;
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}