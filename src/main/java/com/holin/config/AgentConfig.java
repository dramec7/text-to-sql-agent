package com.holin.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author holin
 */
@Slf4j
@Configuration
public class AgentConfig {

    @Bean
    public Agent agent(ChatModel deepseekModel,
                       ToolProvider mcpToolProvider,
                       ChatMemoryProvider chatMemoryProvider,
                       ContentRetriever contentRetriever) {

        Agent agent = AiServices.builder(Agent.class)
                .chatModel(deepseekModel)
                .toolProvider(mcpToolProvider)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();

        log.info("Agent start...");
        return agent;
    }
}
