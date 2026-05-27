package com.cmms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * Object Storage(S3 호환) 설정 바인딩. application.yml `cloud.aws.*` (값은 STORAGE_* env).
 * 기존 죽은 설정을 실제 바인딩으로 전환(9단계 P0).
 */
@ConfigurationProperties(prefix = "cloud.aws")
public record StorageProperties(
        Credentials credentials,
        S3 s3,
        Region region,
        String endpoint,
        String allowedMimes
) {
    public record Credentials(String accessKey, String secretKey) {}

    public record S3(String bucket) {}

    /** region.static 은 예약어이므로 @Name 으로 바인딩명을 매핑한다. */
    public record Region(@Name("static") String staticRegion, boolean auto) {}
}
