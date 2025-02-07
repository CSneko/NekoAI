package org.cneko.ai.core;

import javax.annotation.Nullable;

public class AIRequest {
    private String query;
    private String userId;
    private String sessionId;
    private String prompt;
    private AIHistory history;

    // 构造函数
    public AIRequest(String query, String userId, String sessionId,@Nullable String prompt, @Nullable AIHistory history) {
        if (query == null || userId == null || sessionId == null){
            throw new IllegalArgumentException("query, userId, sessionId cannot be null");
        }
        this.query = query;
        this.userId = userId;
        this.sessionId = sessionId;
        this.prompt = prompt;
        this.history = history;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public @Nullable String getPrompt() {
        return prompt;
    }

    public void setPrompt(@Nullable String prompt) {
        this.prompt = prompt;
    }

    public @Nullable AIHistory getHistory() {
        return history;
    }

    public void setHistory(@Nullable AIHistory history) {
        this.history = history;
    }
}
