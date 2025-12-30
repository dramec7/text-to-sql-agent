package com.holin.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class RabbitMQConfig {

    public static final String SUMMARY_QUEUE = "chat.summary.queue";

    @Bean
    public Queue summaryQueue() {
        // durable=true: RabbitMQ 重启后队列还在
        return new Queue(SUMMARY_QUEUE, true);
    }

    /**
     * 🔥 关键配置：自定义监听容器工厂
     * 作用：让 @RabbitListener 使用虚拟线程来执行任务，而不是占用传统的线程池。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 复用 Spring Boot 的自动配置 (序列化等)
        configurer.configure(factory, connectionFactory);

        // 手动指定执行器：使用 SimpleAsyncTaskExecutor 并开启虚拟线程支持
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-virtual-");
        executor.setVirtualThreads(true); // 开启虚拟线程

        factory.setTaskExecutor(executor);

        return factory;
    }
}