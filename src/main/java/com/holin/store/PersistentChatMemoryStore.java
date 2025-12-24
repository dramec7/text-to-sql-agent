package com.holin.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holin.entity.ChatMemoryDo;
import com.holin.mapper.ChatMemoryMapper;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author holin
 * @date 2025/12/22
 */
@Slf4j
@Configuration
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();

        // 1. 先查 Redis (热数据)
        List<ChatMessage> redisMessages = redisChatMemoryStore.getMessages(sessionId);
        if (redisMessages != null && !redisMessages.isEmpty()) {
            log.info("Hit Redis cache for session: {}", sessionId);
            return redisMessages;
        }

        // 2. Redis 没有，查 MySQL (冷数据 - 只有过期或重启后才会走到这)
        log.info("Cache miss for session: {}, fetching from MySQL...", sessionId);
        List<ChatMemoryDo> history = chatMemoryMapper.selectList(
                new LambdaQueryWrapper<ChatMemoryDo>()
                        .eq(ChatMemoryDo::getSessionId, sessionId)
                        .orderByAsc(ChatMemoryDo::getId)
                        // 策略：只捞最后 20 条恢复上下文，避免太久远的记录浪费 Token
                        .last("LIMIT 20")
        );

        if (history.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 转换格式
        List<ChatMessage> dbMessages = history.stream()
                .map(log -> ChatMessageDeserializer.messageFromJson(log.getContent())) // 假设 content 存的是 JSON
                .collect(Collectors.toList());

        // 4. 回填 Redis (复活记忆)
        redisChatMemoryStore.updateMessages(sessionId, dbMessages);

        return dbMessages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();

        // 1. 同步更新 Redis (保持热度)
        redisChatMemoryStore.updateMessages(sessionId, messages);

        // 2. 异步/同步 写入 MySQL (持久化)
        // 难点：LangChain4j 每次给的是整个 List (例如20条)。
        // 我们不能每次都把20条插一遍。我们需要判断哪些是“新消息”。
        // 简单策略：只持久化列表中的最后一条 (假设是增量对话)
        // 严谨策略：应该在 Controller 层控制入库，或者在这里做去重判断。

        // 为了演示简单，这里假设我们只把“最新产生”的那一条存入 MySQL
        if (!messages.isEmpty()) {
            ChatMessage latestMsg = messages.getLast();
            saveToMySQL(sessionId, latestMsg);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisChatMemoryStore.deleteMessages(memoryId);
        // MySQL 是否删除取决于业务（通常不删，做软删除）
    }

    private void saveToMySQL(String sessionId, ChatMessage message) {
        // 这里需要把 ChatMessage 转成 JSON 字符串存 content 字段
        // 或者拆解字段存 role, text
        String json = ChatMessageSerializer.messageToJson(message);

        ChatMemoryDo entity = ChatMemoryDo.builder()
                .sessionId(sessionId)
                .role(message.type().toString())
                .content(json) // 偷懒做法：直接存 JSON，恢复时反序列化最方便
                .createdAt(java.time.LocalDateTime.now())
                .build();

        chatMemoryMapper.insert(entity);
    }


}
