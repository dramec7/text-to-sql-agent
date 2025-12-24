package com.holin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author holin
 * @date 2025/12/22
 */
@Data
@Builder
@TableName("chat_memory")
public class ChatMemoryDo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String toolCalls; // 存储工具调用的 JSON
    private LocalDateTime createdAt;
}
