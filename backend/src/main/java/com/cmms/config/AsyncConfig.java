package com.cmms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 설정. 파일 삭제 시 커밋 후 S3 객체 제거를 @Async("s3TaskExecutor")로 처리(9단계 P0).
 * @EnableScheduling: 고아 객체 reconciliation @Scheduled 잡(P4)용.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "s3TaskExecutor")
    public Executor s3TaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("s3-async-");
        executor.initialize();
        return executor;
    }
}
