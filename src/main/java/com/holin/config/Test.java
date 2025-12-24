package com.holin.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * @author holin
 * @date 2025/12/23
 */
public class Test {
    public static void main(String[] args) {

        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-2e0ce96094c94595ac089fa7befab770")
                .modelName("text-embedding-v4")
                .logRequests(true)
                .logResponses(true)
                .build();

        Response<Embedding> response = model.embed("Hello World");
        System.out.println("维度: " + response.content().dimension());
        System.out.println("调用成功！");
    }
}
