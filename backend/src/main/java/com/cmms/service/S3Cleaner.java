package com.cmms.service;

import com.cmms.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 커밋 후 S3 객체 비동기 제거. 실패는 로깅만(고아 객체는 P4 reconciliation에서 정리).
 */
@Component
public class S3Cleaner {

    private static final Logger log = LoggerFactory.getLogger(S3Cleaner.class);

    private final S3Client s3;
    private final String bucket;

    public S3Cleaner(S3Client s3, StorageProperties props) {
        this.s3 = s3;
        this.bucket = props.s3().bucket();
    }

    @Async("s3TaskExecutor")
    public void deleteQuietly(String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (Exception e) {
            log.error("S3 객체 제거 실패(고아 가능): bucket={}, key={}", bucket, key, e);
        }
    }
}
