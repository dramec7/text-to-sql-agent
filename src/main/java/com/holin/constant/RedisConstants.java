package com.holin.constant;

public class RedisConstants {
    private RedisConstants() {}

    // 摘要存储 Key
    public static final String SUMMARY_KEY_PREFIX = "chat:summary:";

    // 待压缩消息暂存 Key
    public static final String PENDING_MSG_PREFIX = "chat:pending:compress:";

    // 会话过期时间 1h
    public static final long SESSION_EXPIRE_SEC = 3600;
}