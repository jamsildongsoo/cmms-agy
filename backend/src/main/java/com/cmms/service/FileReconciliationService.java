package com.cmms.service;

import com.cmms.config.StorageProperties;
import com.cmms.repository.FileAttachmentItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * 고아 S3 객체 정리(P4 안전망). 메타(file_attachment_item) 없이 버킷에 남은 객체를
 * 유예시간 경과 후 제거한다. 발생 원인: 삭제 시 @Async S3 제거 실패, 업로드 중단 보상삭제 실패 등.
 * 기본 비활성(cloud.aws.reconcile-enabled=false). 첨부 전용 버킷 가정.
 */
@Service
public class FileReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(FileReconciliationService.class);

    private final S3Client s3;
    private final FileAttachmentItemRepository itemRepo;
    private final String bucket;
    private final boolean enabled;
    private final long graceHours;

    public FileReconciliationService(S3Client s3, StorageProperties props,
                                     FileAttachmentItemRepository itemRepo,
                                     @Value("${cloud.aws.reconcile-enabled:false}") boolean enabled,
                                     @Value("${cloud.aws.reconcile-grace-hours:24}") long graceHours) {
        this.s3 = s3;
        this.itemRepo = itemRepo;
        this.bucket = props.s3().bucket();
        this.enabled = enabled;
        this.graceHours = graceHours;
    }

    @Scheduled(cron = "${cloud.aws.reconcile-cron:0 0 4 * * *}")
    @Transactional(readOnly = true)
    public void reconcile() {
        if (!enabled) {
            return;
        }
        Set<String> known = new HashSet<>(itemRepo.findAllStoragePaths());
        Instant cutoff = Instant.now().minus(graceHours, ChronoUnit.HOURS);

        int scanned = 0;
        int removed = 0;
        String token = null;
        try {
            do {
                final String t = token;
                ListObjectsV2Response resp = s3.listObjectsV2(
                        ListObjectsV2Request.builder().bucket(bucket).continuationToken(t).build());

                for (S3Object obj : resp.contents()) {
                    scanned++;
                    if (known.contains(obj.key())) {
                        continue;
                    }
                    // 유예: 진행 중(메타 미저장 직전) 업로드를 고아로 오인하지 않도록 최근 객체 보호
                    if (obj.lastModified() != null && obj.lastModified().isAfter(cutoff)) {
                        continue;
                    }
                    try {
                        s3.deleteObject(b -> b.bucket(bucket).key(obj.key()));
                        removed++;
                    } catch (Exception e) {
                        log.error("고아 객체 제거 실패: key={}", obj.key(), e);
                    }
                }
                token = resp.isTruncated() ? resp.nextContinuationToken() : null;
            } while (token != null);

            log.info("S3 reconciliation 완료: bucket={}, scanned={}, orphanRemoved={}", bucket, scanned, removed);
        } catch (Exception e) {
            log.error("S3 reconciliation 실패: bucket={}", bucket, e);
        }
    }
}
