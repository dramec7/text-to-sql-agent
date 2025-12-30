# 🚀 Intelligent Text-to-SQL Agent

## 📖 项目简介

这是一个基于 **Java 21** 和 **LangChain4j** 构建的高性能智能数据库查询助手。项目利用大模型（DeepSeek）将自然语言转换为 SQL 语句，并结合 **Schema RAG**（检索增强生成）技术精准定位表结构，支持复杂业务场景下的即席查询。

系统采用了 **热/冷混合记忆架构** 与 **异步摘要压缩机制**，并全面启用 **虚拟线程（Virtual Threads）**，在处理高并发 IO 密集型任务（如 LLM 推理、数据库交互）时，相比传统线程池方案实现了数倍的吞吐量提升。

## 🛠 技术栈

- **核心框架**: Java 21, Spring Boot 3
- **AI 编排**: LangChain4j
- **大模型**: DeepSeek V3.2
- **数据存储**:
  - **Redis**: 短期热点记忆 & 消息暂存
  - **MySQL**: 长期历史归档 & 业务数据
  - **Milvus**: 向量数据库 (存储表结构 Schema Embedding)
- **消息队列**: RabbitMQ
- **性能特性**: Java Virtual Threads (虚拟线程)
