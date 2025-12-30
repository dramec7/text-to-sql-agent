package com.holin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/test/vt")
public class VirtualThreadTestController {

    // 1. 传统线程池 (模拟 Tomcat 默认只有 200 个线程的情况)
    // 这是一个静态的、大小固定的池子
    private final ExecutorService platformPool = Executors.newFixedThreadPool(200);

    // 2. 虚拟线程池 (无限并发)
    // 这是一个特殊的 Executor，每次 submit 都会创建一个新的虚拟线程
    private final ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor();

    @GetMapping("/blast")
    public String blast(@RequestParam String type, @RequestParam(defaultValue = "1000") int count) throws InterruptedException {
        log.info("🔥 [开始] 压测模式: {}, 任务数: {}", type, count);

        // 这是一个倒计时锁，初始值为任务总数 (比如 1000)
        CountDownLatch latch = new CountDownLatch(count);

        // 选线程池
        ExecutorService executor = "virtual".equals(type) ? virtualPool : platformPool;

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    // 执行模拟任务
                    doTask();
                } finally {
                    // 🔥 关键：不管成功失败，每跑完一个，倒计时减 1
                    latch.countDown();
                }
            });
        }

        // 🔥 关键：主线程在这里死等，直到倒计时变成 0 (也就是所有任务都跑完了)
        latch.await();

        long end = System.currentTimeMillis();
        long totalTime = end - start;

        String result = String.format("压测结束！模式: [%s], 任务数: [%d], 总耗时: [%d ms] (%.2f 秒)",
                type, count, totalTime, totalTime / 1000.0);

        log.info(result);
        return result;
    }

    private void doTask() {
        try {
            // 模拟 IO 阻塞 2 秒 (模拟调用 DeepSeek/数据库)
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}