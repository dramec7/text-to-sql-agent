package com.holin.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author holin
 * @date 2025/12/22
 */
@Configuration
public class AgentConfig {

    @Bean
    public Agent agent(ChatModel deepseekModel,
                       ToolProvider mcpToolProvider,
                       ChatMemoryProvider chatMemoryProvider,
                       ContentRetriever contentRetriever) {

        return AiServices.builder(Agent.class)
                .chatModel(deepseekModel)
                .toolProvider(mcpToolProvider)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }
}
