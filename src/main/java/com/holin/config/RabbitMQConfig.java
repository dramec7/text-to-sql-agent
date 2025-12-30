package com.holin.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SUMMARY_QUEUE = "chat.summary.queue";

    @Bean
    public Queue summaryQueue() {
        return new Queue(SUMMARY_QUEUE, true);
    }
}