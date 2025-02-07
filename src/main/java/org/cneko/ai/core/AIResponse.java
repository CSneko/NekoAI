package org.cneko.ai.core;

import javax.annotation.Nullable;

public class AIResponse {
    private String think;
    private String response;
    private boolean success;

    /**
     * 构造函数
     * 如果响应文本符合格式：
     * <think>
     * 思考内容
     * </think>
     * 回复内容
     * 则将思考内容提取到think字段，将回复内容提取到response字段。
     * 如果文本不是这个格式，则think为null，response为原始文本。
     *
     * @param response 响应文本
     * @param success  是否成功
     */
    public AIResponse(String response, boolean success) {
        this.success = success;
        if (response != null && response.trim().startsWith("<think>")) {
            int startIndex = response.indexOf("<think>");
            int endIndex = response.indexOf("</think>");
            // 如果找到了完整的标签，则解析出think和回复内容
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                // 提取think字段内容，去除两边的空白字符
                this.think = response.substring(startIndex + "<think>".length(), endIndex).trim();
                // 提取</think>标签后面的回复内容，并去除空白字符
                this.response = response.substring(endIndex + "</think>".length()).trim();
            } else {
                // 不符合格式，直接将原始文本作为response，think置为null
                this.think = null;
                this.response = response;
            }
        } else {
            // 如果不以<think>开头，则认为文本不符合格式
            this.think = null;
            this.response = response;
        }
    }

    // Getter 和 Setter

    public boolean hasThink() {
        return think != null;
    }

    public @Nullable String getThink() {
        return think;
    }

    public void setThink(String think) {
        this.think = think;
    }

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
