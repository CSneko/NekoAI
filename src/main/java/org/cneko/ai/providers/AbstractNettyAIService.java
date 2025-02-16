package org.cneko.ai.providers;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.cneko.ai.core.*;
import org.cneko.ai.util.FileStorageUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class AbstractNettyAIService<T extends AbstractAIConfig> implements AIService {
    protected final T config;
    protected static final Gson gson = new Gson();
    protected final EventLoopGroup workerGroup = new NioEventLoopGroup();
    protected final SslContext sslContext;

    public AbstractNettyAIService(T config) throws Exception {
        this.config = config;
        this.sslContext = SslContextBuilder.forClient().build();
    }

    protected AIHistory prepareHistory(AIRequest request) {
        AIHistory history = request.getHistory();
        if (history == null) {
            history = new AIHistory();
        }
        if (request.getPrompt() != null) {
            // 添加提示词内容
            AIHistory.Content firstContent = AIHistory.Content.create(
                    AIHistory.Content.Role.USER,
                    "System prompt:" + request.getPrompt() + "\nIf you accept them,please response \"I accept all\""
            );
            AIHistory.Content secondContent = AIHistory.Content.create(
                    AIHistory.Content.Role.MODEL,
                    "I accept all."
            );
            // 注意先添加第二条，再添加第一条到最前面
            history.getContents().add(0,secondContent);
            history.getContents().add(0,firstContent);
        }
        // 添加当前用户请求
        history.getContents().add(AIHistory.Content.create(AIHistory.Content.Role.USER, request.getQuery()));
        return history;
    }

    protected void configurePipeline(SocketChannel ch, ChannelHandler responseHandler) {
        ch.pipeline().addLast(
                sslContext.newHandler(ch.alloc(), config.getHost(), config.getPort()),
                new HttpClientCodec(),
                new HttpObjectAggregator(65536),
                responseHandler
        );
    }

    protected void saveConversation(AIRequest request, String responseText) {
        try {
            FileStorageUtil.saveConversation(
                    request.getUserId(),
                    request.getSessionId(),
                    request.getQuery(),
                    responseText
            );
        } catch (Exception e) {
            // 可以记录日志，但不影响最终结果
        }
    }




    @Override
    public AIResponse processRequest(AIRequest request) {
        CompletableFuture<AIResponse> future = new CompletableFuture<>();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 如果配置了代理，则添加代理处理器
                            NetworkingProxy proxy = config.getProxy();
                            if (proxy != null) {
                                ch.pipeline().addLast(new HttpProxyHandler(
                                        new InetSocketAddress(proxy.getIp(), proxy.getPort())));
                            }
                            // 调用子类实现的通道初始化方法
                            AbstractNettyAIService.this.initChannel(ch, request, future);
                        }
                    });

            // 连接目标主机和端口
            bootstrap.connect(getHost(), getPort()).addListener(f -> {
                if (f.isSuccess()) {
                    Channel channel = ((ChannelFuture) f).channel();
                    sendRequest(channel, request);
                } else {
                    future.complete(new AIResponse("Connection failed: " + f.cause().getMessage(), 400));
                }
            });
        } catch (Exception e) {
            return new AIResponse("Error: " + e.getMessage(), 400);
        }

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return new AIResponse("Request interrupted: " + e.getMessage(), 400);
        }
    }

    /**
     * 子类实现：在流水线中添加需要的处理器
     */
    protected abstract void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future);

    /**
     * 子类实现：发送 HTTP 请求的逻辑
     */
    protected abstract void sendRequest(Channel channel, AIRequest request);

    /**
     * 目标主机地址
     */
    protected String getHost(){
        return config.getHost();
    }

    /**
     * 目标端口号
     */
    protected int getPort(){
        return config.getPort();
    }
}
