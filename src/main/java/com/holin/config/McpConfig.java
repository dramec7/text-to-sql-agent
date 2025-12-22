//package com.holin.config;
//
//import dev.langchain4j.mcp.McpToolProvider;
//import dev.langchain4j.mcp.client.DefaultMcpClient;
//import dev.langchain4j.mcp.client.McpClient;
//import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import java.time.Duration;
//
///**
// * @author holin
// * @date 2025/12/21
// */
//@Configuration
//public class McpConfig {
//
//    @Bean(destroyMethod = "close")
//    public McpClient mcpClient(){
//
//        HttpMcpTransport transport = new HttpMcpTransport.Builder()
//                .sseUrl("http://localhost:8080/sse")
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//
//        McpClient client = new DefaultMcpClient.Builder()
//                .transport(transport)
//                .clientName("spring-boot-agent")
//                .clientVersion("1.0.0")
//                .toolExecutionTimeout(Duration.ofSeconds(200))
//                .build();
//
//        System.out.println("⏳ 正在连接 MCP Server (SSE)...");
//        try {
//            var tools = client.listTools();
//            System.out.println("✅ MCP Server 连接成功！已发现工具: \" + tools.size() + \" 个\"");
//        } catch (Exception e) {
//            throw new RuntimeException("❌ 无法连接到 Text-to-SQL MCP Server，请检查端口是否启动", e);
//        }
//
//        return client;
//    }
//
//    @Bean
//    public McpToolProvider mcpToolProvider(McpClient mcpClient) {
//        return McpToolProvider.builder()
//                .mcpClients(mcpClient)
//                .build();
//    }
//}
