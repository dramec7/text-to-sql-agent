package com.holin.service;

import com.holin.entity.ChatMemoryDo;
import com.holin.mapper.ChatMemoryMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    @Async
    public void saveToMySQL(String sessionId, ChatMessage message) {
        try {
            // 序列化
            String json = ChatMessageSerializer.messageToJson(message);

            ChatMemoryDo entity = ChatMemoryDo.builder()
                    .sessionId(sessionId)
                    .role(message.type().toString())
                    .content(json)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            chatMemoryMapper.insert(entity);

             log.debug("消息已异步归档至 MySQL, SessionId: {}", sessionId);

        } catch (Exception e) {
            log.error("消息异步归档失败 (不影响主流程)", e);
        }
    }
}