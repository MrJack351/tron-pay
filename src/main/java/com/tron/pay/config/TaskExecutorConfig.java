package com.tron.pay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableAsync
public class TaskExecutorConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30); // 设置线程池的基本大小
        executor.setMaxPoolSize(100); // 设置线程池的最大大小
        executor.setQueueCapacity(50); // 设置线程池的队列容量
        executor.setKeepAliveSeconds(60); // 设置线程的空闲时间
        executor.setThreadNamePrefix("Async-"); // 设置线程的名称前缀
        executor.initialize();
        return executor;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        // 使用@Bean注解创建ScheduledExecutorService实例
        return Executors.newScheduledThreadPool(30);
    }

}