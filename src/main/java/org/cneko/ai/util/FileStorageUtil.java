package org.cneko.ai.util;

import com.google.gson.Gson;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIHistory.Content;
import org.cneko.ai.core.AIHistory.Content.Role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.cneko.ai.NekoLogger.LOGGER;

public class FileStorageUtil {
    private static final Gson gson = new Gson();
    private static final String BASE_PATH = "ai/data/";

    /**
     * 保存会话数据：将用户的问题和AI的回复追加到已有会话记录中
     *
     * @param userId    用户ID
     * @param sessionId 会话标识符
     * @param query     用户提问
     * @param response  AI回复
     */
    public static void saveConversation(String userId, String sessionId, String query, String response) {
        try {
            // 构建存储目录及文件路径
            Path dirPath = Paths.get(BASE_PATH, userId);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(sessionId + ".json");

            // 读取已有的会话记录
            AIHistory history = readConversation(userId, sessionId);
            if (history == null) {
                history = new AIHistory();
                history.setContents(new ArrayList<>());
            }

            // 使用 AIHistory.Content.create 创建新的消息内容，并添加到记录中
            history.getContents().add(Content.create(Role.USER, query));
            history.getContents().add(Content.create(Role.MODEL, response));

            // 将 AIHistory 对象序列化为 JSON 并写入文件
            String json = gson.toJson(history);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save conversation:{}",e.getMessage());
        }
    }

    /**
     * 读取指定用户和会话的历史记录
     *
     * @param userId    用户ID
     * @param sessionId 会话标识符
     * @return AIHistory 对象，若记录不存在则返回 null
     */
    public static AIHistory readConversation(String userId, String sessionId) {
        try {
            Path filePath = Paths.get(BASE_PATH, userId, sessionId + ".json");
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                return AIHistory.fromJson(json);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read conversation：{}",e.getMessage());
        }
        return null;
    }
}
