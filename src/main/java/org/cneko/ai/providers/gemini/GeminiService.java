package org.cneko.ai.providers.gemini;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.AbstractNettyAIService;
import org.cneko.ai.util.FileStorageUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.cneko.ai.NekoLogger.LOGGER;

public class GeminiService extends AbstractNettyAIService<GeminiConfig> {

    public GeminiService(GeminiConfig config) throws Exception {
        super(config);
    }

    /**
     * 初始化 Channel，添加 SSL、HTTP 编解码器、聚合器及自定义的响应处理器
     */
    @Override
    protected void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future) {
        configurePipeline(ch, new GeminiHandler(request, future));
    }


    /**
     * 发送 Gemini 请求，使用 AIHistory 对象构造请求体
     */
    @Override
    protected void sendRequest(Channel channel, AIRequest request) {
        // 调用公共方法构造历史记录
        AIHistory history = prepareHistory(request);
        // Gemini 接口直接使用 AIHistory 的 JSON 表示
        String jsonBody = history.toJson();
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bodyBytes);

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                "/v1beta/models/" + config.getModel() + ":generateContent?key=" + config.getApiKey(),
                buf
        );
        httpRequest.headers()
                .set(HttpHeaderNames.HOST, config.getHost())
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());

        channel.writeAndFlush(httpRequest);
    }




    /**
     * Gemini 内部的响应处理 Handler
     */
    private static class GeminiHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final AIRequest request;
        private final CompletableFuture<AIResponse> future;

        public GeminiHandler(AIRequest request, CompletableFuture<AIResponse> future) {
            this.request = request;
            this.future = future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            String content = response.content().toString(StandardCharsets.UTF_8);

            if (response.status().code() != HttpResponseStatus.OK.code()) {
                future.complete(new AIResponse("API Error: " + content, false));
                return;
            }

            try {
                // 使用 GeminiResponse 类型替代 Map 解析
                GeminiResponse responseObj = gson.fromJson(content, GeminiResponse.class);

                // 获取第一个候选内容
                GeminiResponse.Candidate firstCandidate = responseObj.getCandidates().stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No candidates found"));

                // 获取 parts 中的第一个文本内容
                String responseText = firstCandidate.getContent().getParts().stream()
                        .map(GeminiResponse.Part::getText)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No text parts found"));

                // 保存会话记录（原有逻辑保持不变）
                try {
                    FileStorageUtil.saveConversation(
                            request.getUserId(),
                            request.getSessionId(),
                            request.getQuery(),
                            responseText
                    );
                } catch (Exception e) {
                    LOGGER.error("Error saving conversation: {}", e.getMessage());
                }

                future.complete(new AIResponse(responseText.trim(), true));
            } catch (Exception e) {
                future.complete(new AIResponse("Response parsing error: " + e.getMessage(), false));
            }
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.complete(new AIResponse("Network error: " + cause.getMessage(), false));
            ctx.close();
        }

        public static class GeminiResponse {
            private List<Candidate> candidates;

            public List<Candidate> getCandidates() {
                return candidates != null ? candidates : Collections.emptyList();
            }

            public static class Candidate {
                private Content content;

                public Content getContent() {
                    return content != null ? content : new Content();
                }
            }

            public static class Content {
                private List<Part> parts;

                public List<Part> getParts() {
                    return parts != null ? parts : Collections.emptyList();
                }
            }

            public static class Part {
                private String text;

                public String getText() {
                    return text != null ? text : "";
                }
            }
        }
    }
}
