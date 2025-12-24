package com.holin.component;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
// 🔥 新增：Milvus SDK 依赖
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.dml.DeleteParam;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealSchemaIngestor {


    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${agent.schema.init:true}")
    private boolean initSchema;

    @Value("${agent.milvus.uri:http://localhost:19530}")
    private String milvusUri;

    @Value("${agent.milvus.collection:schema_rag_collection}")
    private String collectionName;

    @PostConstruct
    public void init() {
        if (!initSchema) {
            log.info("Schema 初始化开关已关闭，跳过扫描。");
            return;
        }

        // 1 清空 Milvus 旧数据
        log.info("正在清理 Milvus 集合 [{}] 中的旧数据...", collectionName);
        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder().withUri(milvusUri).build()
        );

        try {
            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr("id != ''")
                    .build());

            log.info("✅ 旧数据清理完成！");
        } catch (Exception e) {
            log.warn("清理 Milvus 数据失败 (如果是首次启动可忽略): {}", e.getMessage());
        } finally {
            try {
                milvusClient.close();
            } catch (Exception ex) {
                log.warn("关闭 Milvus 连接失败", ex);
            }
        }

        // 2 扫描数据库 Schema
        log.info("🚀 开始扫描 MySQL 数据库 Schema...");
        List<Document> documents = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取所有表
            ResultSet tables = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableComment = tables.getString("REMARKS");

                if (tableName.startsWith("sys_") || tableName.equals("chat_memory")) {
                    continue;
                }

                StringBuilder schemaText = new StringBuilder();
                schemaText.append("Table Name: ").append(tableName).append("\n");
                schemaText.append("Description: ").append(tableComment == null ? "无" : tableComment).append("\n");
                schemaText.append("Columns:\n");

                ResultSet columns = metaData.getColumns(conn.getCatalog(), null, tableName, "%");
                while (columns.next()) {
                    String colName = columns.getString("COLUMN_NAME");
                    String colType = columns.getString("TYPE_NAME");
                    String colRemark = columns.getString("REMARKS");

                    schemaText.append("- ").append(colName).append(" (").append(colType).append(")");
                    if (colRemark != null && !colRemark.isEmpty()) {
                        schemaText.append(": ").append(colRemark);
                    }
                    schemaText.append("\n");
                }

                log.info("扫描到表: {}", tableName);
                Document doc = Document.from(schemaText.toString(), Metadata.from("tableName", tableName));
                documents.add(doc);
            }

        } catch (Exception e) {
            log.error("扫描数据库失败", e);
            return;
        }

        if (documents.isEmpty()) {
            log.warn("未扫描到任何表！请检查数据库连接。");
            return;
        }

        // 3 重新入库
        log.info("正在将 {} 张表的元数据重新写入 Milvus...", documents.size());

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);

        log.info("✅ Schema RAG 全量刷新完成！");
    }
}