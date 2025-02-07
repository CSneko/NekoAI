package org.cneko.ai.core;

public class AIResponse {
    private String response;
    private boolean success;

    // 构造函数
    public AIResponse(String response, boolean success) {
        this.response = response;
        this.success = success;
    }

    // Getter 和 Setter
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
