package com.holin.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * @author holin
 * @date 2025/12/21
 */
@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

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

        try {
            var tools = client.listTools();
            log.info("MCP Server connected, found {} tools", tools.size());
        } catch (Exception e) {
            log.error("fail to connect to MCP server", e);
            throw new RuntimeException("fail to connect to MCP server", e);
        }

        return client;
    }

    @Bean
    public ToolProvider mcpToolProvider(McpClient mcpClient) {
        log.info("MCP Tool Provider start...");
        return McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
    }
}
