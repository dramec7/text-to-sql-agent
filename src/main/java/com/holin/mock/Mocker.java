package com.holin.mock;

import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author holin
 */
@Slf4j
public class Mocker {
    private void doCompress(String sessionId, List<ChatMessage> messagesToCompress) {
        try {
            log.info("模拟调用 DeepSeek，线程: {}", Thread.currentThread());
            // 🔥 关键：模拟 IO 阻塞 2 秒（模拟大模型思考）
            // 在传统线程下，这会占死一个线程 2秒。
            // 在虚拟线程下，这会立刻挂起，不占 CPU。
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("模拟完成");
    }
}
