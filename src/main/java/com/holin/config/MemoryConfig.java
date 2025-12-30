package com.holin.config;

import com.holin.store.PersistentChatMemoryStore;
import dev.langchain4j.community.store.ememory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author holin
 */
@Configuration
public class MemoryConfig {

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host("localhost")
                .port(6379)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(PersistentChatMemoryStore persistentChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(persistentChatMemoryStore)
                .build();
    }

}
