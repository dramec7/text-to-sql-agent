package com.holin.config;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @author holin
 */
public interface Agent {

    @SystemMessage("你是一个数据库管理员。")
    String query(@MemoryId String userId, @UserMessage String question);

}
