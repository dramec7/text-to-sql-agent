package com.holin.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * @author holin
 */
@Slf4j
@Configuration
public class McpConfig {

    @Bean
    public McpClient mcpClient(){
        HttpMcpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:8080/sse")
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient client = new DefaultMcpClient.Builder()
                .transport(transport)
                .clientName("text-to-sql-agent")
                .clientVersion("1.0.0")
                .toolExecutionTimeout(Duration.ofSeconds(3600))
                .build();

        log.info("MCP Server start...");
        return client;
    }

    @Bean
    public ToolProvider mcpToolProvider(McpClient mcpClient) {
        return McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
    }
}
