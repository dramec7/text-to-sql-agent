package com.holin.config;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @author holin
 * @date 2025/12/22
 */
public interface Agent {

    @SystemMessage("""
        你是一个专业且友好的 AI 助手，请根据用户问题给出回答。
        """)
    String query(@UserMessage String question);

}
