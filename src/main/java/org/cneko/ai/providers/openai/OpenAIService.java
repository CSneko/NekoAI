package org.cneko.ai.providers.openai;

import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.AbstractNettyAIService;
import org.cneko.ai.util.FileStorageUtil;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.cneko.ai.NekoLogger.LOGGER;

/**
 * OpenAIService 是一个通用的服务类，用于调用 OpenAI 格式的聊天补全接口，
 * 其请求体中使用 "messages" 字段，消息内容由 AIHistory 对象转换而来，转换规则为：
 *   - AIHistory.Content.Role.USER 转换为 "user"
 *   - 其他角色转换为 "assistant"
 */
public class OpenAIService extends AbstractNettyAIService<OpenAIConfig> {


    public OpenAIService(OpenAIConfig config) throws Exception {
        super(config);
    }

    /**
     * 初始化 Channel，根据配置动态添加 SSL 处理器
     */
    @Override
    protected void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future) {
        // 添加通用处理器
        configurePipeline(ch, new CommonChatHandler(request, future));
    }


    /**
     * 发送请求，将 AIHistory 转换为 API 所需的 "messages" 格式，并调用配置中指定的 endpoint
     */
    @Override
    protected void sendRequest(Channel channel, AIRequest request) {
        // 构造对话历史记录
        AIHistory history = prepareHistory(request);
        // 转换为 API 所需的 messages 格式
        List<Map<String, Object>> messages = convertHistoryToMessages(history);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", messages);

        String jsonBody = gson.toJson(requestBody);
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bodyBytes);

        // 根据配置文件中的 endpoint（如 "/v1/chat/completions" 或 "/openai/v1/chat/completions"）构造请求
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                config.getEndpoint(),
                buf
        );
        httpRequest.headers()
                .set(HttpHeaderNames.HOST, config.getHost())
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes())
                .set(HttpHeaderNames.AUTHORIZATION, "Bearer " + config.getApiKey());

        channel.writeAndFlush(httpRequest);
    }

    /**
     * 将 AIHistory 转换为 API 所需的 messages 格式
     */
    protected List<Map<String, Object>> convertHistoryToMessages(AIHistory history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (AIHistory.Content content : history.getContents()) {
            Map<String, Object> message = new HashMap<>();
            // 根据角色进行转换：若为 USER 则设为 "user"，否则视为 "assistant"
            if (content.getRole() == AIHistory.Content.Role.USER) {
                message.put("role", "user");
            } else {
                message.put("role", "assistant");
            }
            // 拼接每个 Part 的文本内容
            StringBuilder sb = new StringBuilder();
            if (content.getParts() != null) {
                for (AIHistory.Content.Part part : content.getParts()) {
                    sb.append(part.getText());
                }
            }
            message.put("content", sb.toString());
            messages.add(message);
        }
        return messages;
    }

    /**
     * 内部响应处理 Handler，用于解析 API 返回的响应数据
     */
    private static class CommonChatHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final AIRequest request;
        private final CompletableFuture<AIResponse> future;

        public CommonChatHandler(AIRequest request, CompletableFuture<AIResponse> future) {
            this.request = request;
            this.future = future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            String content = response.content().toString(StandardCharsets.UTF_8);

            // 若状态码不为 200，则直接返回错误信息
            int statusCode = response.status().code();
            if (statusCode != HttpResponseStatus.OK.code()) {
                future.complete(new AIResponse("API Error: " + content, statusCode));
                return;
            }

            try {
                // 解析响应 JSON 为 Map 对象
                Map<String, Object> responseMap = gson.fromJson(
                        content,
                        new TypeToken<Map<String, Object>>() {}.getType()
                );
                // 获取 choices 数组
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices == null || choices.isEmpty()) {
                    future.complete(new AIResponse("No valid response from API", statusCode));
                    return;
                }
                // 取第一个 candidate 的 message 对象
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message == null) {
                    future.complete(new AIResponse("No message in API response", statusCode));
                    return;
                }

                // 同时获取 content 和 reasoning_content
                String responseText = (String) message.get("content");
                String reasoningContent = (String) message.get("reasoning_content");

                if (responseText == null || responseText.isEmpty()) {
                    future.complete(new AIResponse("Empty response content", statusCode));
                    return;
                }

                // 构建要保存的文本内容
                StringBuilder saveContentBuilder = new StringBuilder();
                if (reasoningContent != null && !reasoningContent.isEmpty()) {
                    saveContentBuilder.append("<think>").append(reasoningContent).append("</think>");
                }
                saveContentBuilder.append(responseText);
                String saveContent = saveContentBuilder.toString();

                // 保存会话记录（包含思考内容）
                try {
                    FileStorageUtil.saveConversation(
                            request.getUserId(),
                            request.getSessionId(),
                            request.getQuery(),
                            saveContent
                    );
                } catch (Exception e) {
                    // 记录日志等处理，不影响返回结果
                }

                // 根据是否存在 reasoning_content 构建不同响应对象
                if (reasoningContent != null && !reasoningContent.isEmpty()) {
                    future.complete(new AIResponse(responseText.trim(), reasoningContent, statusCode));
                } else {
                    future.complete(new AIResponse(responseText.trim(), statusCode));
                }
            } catch (Exception e) {
                future.complete(new AIResponse("Response parsing error: " + e.getMessage(), statusCode));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.complete(new AIResponse("Network error: " + cause.getMessage(), 400));
            ctx.close();
        }
    }
}
