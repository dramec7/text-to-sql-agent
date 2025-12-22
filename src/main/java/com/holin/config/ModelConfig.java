package com.holin.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author holin
 * @date 2025/12/22
 */
@Configuration
public class ModelConfig {

    @Bean
    public ChatModel DeepseekModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-2c190100645d4ad3b2c635f7a81854ac")
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
