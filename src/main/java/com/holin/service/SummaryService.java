package com.holin.service;

import com.holin.config.RabbitMQConfig;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.*; //
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.holin.constant.RedisConstants;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SummaryService{

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*
    生产者方法：触发压缩任务
     */
    public void triggerCompression(String sessionId, List<ChatMessage> messagesToCompress) {
        log.info("触发异步压缩任务, SessionId: {}", sessionId);

        // 1. RabbitMQ 不适合传大包数据。先把待压缩的消息序列化存入 Redis 临时区域
        String jsonMessages = ChatMessageSerializer.messagesToJson(messagesToCompress);

        // 存入 Redis，设置较短的过期时间 (比如 5 分钟)，防止任务堆积导致内存泄漏
        redisTemplate.opsForValue().set(RedisConstants.PENDING_MSG_PREFIX + sessionId, jsonMessages, 5, TimeUnit.MINUTES);

        // 2. 发送轻量级的 SessionId 到 MQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.SUMMARY_QUEUE, sessionId);
    }

    /*
    消费者方法：处理压缩任务
     */
    @RabbitListener(queues = RabbitMQConfig.SUMMARY_QUEUE, concurrency = "2-5")
    public void handleCompressionTask(String sessionId) {
        log.info("MQ 收到压缩任务，SessionId: {}", sessionId);

        try {
            // 1. 从 Redis 捞回待压缩的消息
            String jsonMessages = redisTemplate.opsForValue().get(RedisConstants.PENDING_MSG_PREFIX + sessionId);

            if (jsonMessages == null) {
                log.warn("未找到待压缩消息 (可能已过期或被处理)，跳过。SessionId: {}", sessionId);
                return;
            }

            // 反序列化回 List<ChatMessage>
            List<ChatMessage> messagesToCompress = ChatMessageDeserializer.messagesFromJson(jsonMessages);

            // 2. 执行核心压缩逻辑 (调用 DeepSeek)
            doCompress(sessionId, messagesToCompress);

            // 3. 任务成功，清理临时数据
            redisTemplate.delete(RedisConstants.PENDING_MSG_PREFIX + sessionId);

        } catch (Exception e) {
            log.error("压缩任务执行失败，SessionId: " + sessionId, e);
            // 如果抛出异常，RabbitMQ 默认会重试 (Requeue)。
            // 如果不想无限重试，可以配置 Dead Letter Queue (死信队列)。
        }
    }

    private void doCompress(String sessionId, List<ChatMessage> messagesToCompress) {
        // 1. 获取旧摘要
        String oldSummary = getSummary(sessionId);

        // 2. 格式化对话文本
        String historyText = messagesToCompress.stream()
                .filter(msg -> msg instanceof UserMessage || msg instanceof AiMessage)
                .map(msg -> {
                    String role;
                    String content;

                    if (msg instanceof UserMessage) {
                        role = "User";
                        content = ((UserMessage) msg).contents().stream()
                                .filter(c -> c instanceof TextContent)
                                .map(c -> ((TextContent) c).text())
                                .collect(Collectors.joining(" "));
                    } else {
                        role = "AI";
                        String text = ((AiMessage) msg).text();
                        // 判空保护：AI 如果只是调用了工具（没说话），text 可能是 null
                        content = (text != null) ? text : "[执行动作/查询]";
                    }

                    // 简单的截断保护
                    if (content != null && content.length() > 500) {
                        content = content.substring(0, 500) + "...";
                    }

                    return role + ": " + content;
                })
                .collect(Collectors.joining("\n"));

        // 3. 构造 Prompt
        String prompt = String.format("""
            你是一个 Text-to-SQL 系统的记忆压缩助手。
            
            【旧摘要】：
            %s
            
            【最近发生的对话】：
            %s
            
            【任务】：
            请结合“旧摘要”和“最近发生的对话”，生成一份新的摘要。
            
            【要求】：
            1. 重点保留：用户关注的**表名**、**字段**、**过滤条件**（如时间范围、状态）、**业务定义**。
            2. 忽略：寒暄、格式错误尝试、报错信息。
            3. 输出格式：直接输出摘要内容，不要加任何前缀。
            """,
                (oldSummary == null ? "无" : oldSummary),
                historyText
        );

        // 4. 调用 LLM
        String newSummary = chatModel.chat(prompt);
        log.info("DeepSeek 生成摘要成功，长度: {}", newSummary.length());

        // 5. 更新 Redis
        saveSummary(sessionId, newSummary);
    }

    public String getSummary(String sessionId) {
        return redisTemplate.opsForValue().get(RedisConstants.SUMMARY_KEY_PREFIX + sessionId);
    }

    public void saveSummary(String sessionId, String summary) {
        if (summary != null && !summary.isEmpty()) {
            redisTemplate.opsForValue().set(RedisConstants.SUMMARY_KEY_PREFIX + sessionId, summary, 1, TimeUnit.HOURS);
        }
    }

    // 清除摘要 (用于删除对话时)
    public void clearSummary(String sessionId) {
        redisTemplate.delete(RedisConstants.SUMMARY_KEY_PREFIX + sessionId);
        redisTemplate.delete(RedisConstants.PENDING_MSG_PREFIX + sessionId);
    }
}