package com.holin.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


/**
 * @author holin
 */
@Configuration
public class ModelConfig {
    @Bean
    public ChatModel DeepseekModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("your-api-key")
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(3600))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
