package com.holin.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holin.entity.ChatMemoryDo;
import com.holin.mapper.ChatMemoryMapper;
import com.holin.service.ArchiveService;
import com.holin.service.SummaryService;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore; // 注意包名可能根据版本不同
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 持久化聊天记忆存储 (Redis + MySQL + 摘要压缩)
 * @author holin
 */
@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private  ChatMemoryMapper chatMemoryMapper;

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private ArchiveService archiveService;

    // 触发压缩的阈值 20
    private static final int COMPRESSION_THRESHOLD = 20;
    // 每次切出去压缩的老消息数量 10
    private static final int COMPRESSION_BATCH_SIZE = 10;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();

        // 1. 获取近期详细对话 (先查 Redis，Miss 则查 MySQL)
        List<ChatMessage> recentMessages = getRecentMessages(sessionId);

        // 2. 获取历史摘要 (Summary)
        String summary = summaryService.getSummary(sessionId);

        // 3. 组装最终上下文：[摘要 SystemMessage] + [近期详细对话]
        if (summary != null && !summary.isBlank()) {
            // 将摘要包装成系统消息，放在最前面
            SystemMessage summaryMsg = SystemMessage.from(
                    "【历史对话摘要】(这是你之前的记忆，请基于此回复，但不要重复提及): " + summary
            );

            // 创建一个新 List，避免修改原引用
            List<ChatMessage> combinedMessages = new ArrayList<>();
            combinedMessages.add(summaryMsg);
            combinedMessages.addAll(recentMessages);

            return combinedMessages;
        }

        return recentMessages;
    }

    /**
     * 获取近期消息的私有方法 (Redis -> MySQL 降级逻辑)
     */
    private List<ChatMessage> getRecentMessages(String sessionId) {
        // A. 查 Redis
        List<ChatMessage> redisMessages = redisChatMemoryStore.getMessages(sessionId);
        if (redisMessages != null && !redisMessages.isEmpty()) {
             log.debug("Hit Redis cache for session: {}", sessionId);
            return redisMessages;
        }

        // B. Redis Miss -> 查 MySQL (恢复上下文)
        log.info("Cache miss for session: {}, fetching from MySQL...", sessionId);
        List<ChatMemoryDo> history = chatMemoryMapper.selectList(
                new LambdaQueryWrapper<ChatMemoryDo>()
                        .eq(ChatMemoryDo::getSessionId, sessionId)
                        .orderByAsc(ChatMemoryDo::getId)
                        // 策略：MySQL 里可能存了几千条，恢复时只取最后 20 条，否则 Token 爆炸
                        .last("LIMIT " + COMPRESSION_THRESHOLD)
        );

        if (history.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMessage> dbMessages = history.stream()
                .map(log -> ChatMessageDeserializer.messageFromJson(log.getContent()))
                .collect(Collectors.toList());

        // C. 回写 Redis (热数据预热)
        redisChatMemoryStore.updateMessages(sessionId, dbMessages);

        return dbMessages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();

        // 1. 调用外部 Bean 的异步方法，实现真正的多线程处理
        if (!messages.isEmpty()) {
            ChatMessage latestMsg = messages.get(messages.size() - 1);
            // 这行代码会立即返回，不会阻塞主线程等待 MySQL 写入
            archiveService.saveToMySQL(sessionId, latestMsg);
        }

        // 2. 检查是否触发压缩
        if (messages.size() >= COMPRESSION_THRESHOLD) {
            triggerCompression(sessionId, messages);
        } else {
            // 没满阈值，正常更新 Redis (Redis 很快，同步执行即可)
            redisChatMemoryStore.updateMessages(sessionId, messages);
        }
    }

    /**
     * 触发压缩逻辑
     */
    private void triggerCompression(String sessionId, List<ChatMessage> currentMessages) {
        log.info("会话 {} 消息数达到 {}，触发历史压缩...", sessionId, currentMessages.size());

        // 1. 切分：前 10 条 (旧) 去压缩，后 10 条 (新) 留着
        // 务必使用 new ArrayList 拷贝，subList 产生的视图不可序列化
        List<ChatMessage> toCompress = new ArrayList<>(currentMessages.subList(0, COMPRESSION_BATCH_SIZE));
        List<ChatMessage> toKeep = new ArrayList<>(currentMessages.subList(COMPRESSION_BATCH_SIZE, currentMessages.size()));

        // 2. 立即更新 Redis (只保留新的 10 条)
        // 这样下一次 getMessages 就只会拿到这 10 条，腾出了 Token 空间
        redisChatMemoryStore.updateMessages(sessionId, toKeep);

        // 3. 发送给 SummaryService (RabbitMQ 异步处理)
        // 这里是非阻塞的
        summaryService.triggerCompression(sessionId, toCompress);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        log.info("正在清除会话 [{}] 的所有记忆...", sessionId);

        // 1. 删 Redis
        redisChatMemoryStore.deleteMessages(memoryId);

        // 2. 删 Summary (必须删，否则新对话会带着旧摘要)
        summaryService.clearSummary(sessionId);

        // 3. 删 MySQL (物理删除)
        chatMemoryMapper.delete(
                new LambdaQueryWrapper<ChatMemoryDo>()
                        .eq(ChatMemoryDo::getSessionId, sessionId)
        );
    }
}