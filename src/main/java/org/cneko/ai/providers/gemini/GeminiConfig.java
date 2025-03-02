package org.cneko.ai.providers.gemini;

import org.cneko.ai.core.NetworkingProxy;
import org.cneko.ai.providers.AbstractAIConfig;

import javax.annotation.Nullable;

public class GeminiConfig extends AbstractAIConfig {
    public GeminiConfig(String apiKey) {
        super(apiKey, "gemini-2.0-flash", 0.7, "generativelanguage.googleapis.com", 443,true);
    }
}
