package com.cmms.service;

import com.cmms.config.StorageProperties;
import com.cmms.dto.FileDto.FileItemResponse;
import com.cmms.dto.FileDto.UploadResponse;
import com.cmms.model.FileAttachment;
import com.cmms.model.FileAttachmentId;
import com.cmms.model.FileAttachmentItem;
import com.cmms.model.FileAttachmentItemId;
import com.cmms.repository.FileAttachmentItemRepository;
import com.cmms.repository.FileAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 파일 업로드/다운로드/삭제. 업로드·다운로드는 백엔드 경유(스트리밍).
 * 삭제는 메타(동기, 트랜잭션 내) + 커밋 후 S3 객체 비동기 제거. 모든 접근은 companyId(테넌트) 격리.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final S3Client s3;
    private final FileAttachmentRepository groupRepo;
    private final FileAttachmentItemRepository itemRepo;
    private final S3Cleaner cleaner;
    private final String bucket;
    private final List<String> allowedMimes;

    public FileStorageService(S3Client s3, StorageProperties props,
                              FileAttachmentRepository groupRepo,
                              FileAttachmentItemRepository itemRepo,
                              S3Cleaner cleaner) {
        this.s3 = s3;
        this.groupRepo = groupRepo;
        this.itemRepo = itemRepo;
        this.cleaner = cleaner;
        this.bucket = props.s3().bucket();
        this.allowedMimes = props.allowedMimes() == null ? List.of()
                : Arrays.stream(props.allowedMimes().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 업로드: groupNo가 있으면 해당 그룹에 추가, 없으면 신규 그룹 생성. */
    @Transactional
    public UploadResponse upload(String companyId, String username, String refModule, String refNo,
                                 Long groupNo, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        FileAttachment group;
        if (groupNo != null) {
            group = groupRepo.findById(new FileAttachmentId(companyId, groupNo))
                    .filter(g -> "N".equals(g.getDeleteYn()))
                    .orElseThrow(() -> new IllegalArgumentException("첨부 그룹을 찾을 수 없습니다."));
        } else {
            group = new FileAttachment();
            group.setCompanyId(companyId);
            group.setRefModule(refModule);
            group.setRefNo(refNo);
            group.setCreatedBy(username);
            group.setUpdatedBy(username);
            group = groupRepo.saveAndFlush(group); // group_no(IDENTITY) 확보
        }

        Long gno = group.getGroupNo();
        String moduleSeg = sanitizeSegment(group.getRefModule());
        int nextItemNo = itemRepo.findMaxItemNo(companyId, gno) + 1;

        List<String> uploadedKeys = new ArrayList<>();
        List<FileItemResponse> result = new ArrayList<>();
        try {
            for (MultipartFile f : files) {
                validate(f);
                String original = baseName(f.getOriginalFilename());
                String ext = extensionOf(original);
                String stored = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
                String key = companyId + "/" + moduleSeg + "/" + gno + "/" + stored;

                byte[] bytes = f.getBytes();
                String sha = sha256(bytes);
                String contentType = f.getContentType();

                s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType), RequestBody.fromBytes(bytes));
                uploadedKeys.add(key);

                FileAttachmentItem item = new FileAttachmentItem();
                item.setCompanyId(companyId);
                item.setGroupNo(gno);
                item.setItemNo(nextItemNo++);
                item.setOriginalFileName(original);
                item.setStoredFileName(stored);
                item.setFileExtension(ext.isEmpty() ? null : ext);
                item.setMimeType(contentType);
                item.setFileSize(f.getSize());
                item.setChecksumSha256(sha);
                item.setStoragePath(key);
                itemRepo.save(item);

                result.add(toResponse(item));
            }
        } catch (Exception e) {
            // 보상: 메타 저장 실패 등으로 트랜잭션이 롤백돼도 S3 객체는 남으므로 즉시 제거(best-effort)
            for (String k : uploadedKeys) {
                try {
                    s3.deleteObject(b -> b.bucket(bucket).key(k));
                } catch (Exception ignore) {
                    log.error("업로드 보상 삭제 실패: key={}", k, ignore);
                }
            }
            if (e instanceof IllegalArgumentException iae) throw iae;
            throw new RuntimeException("파일 업로드 처리 중 오류가 발생했습니다.", e);
        }

        return new UploadResponse(gno, result);
    }

    @Transactional(readOnly = true)
    public List<FileItemResponse> list(String companyId, Long groupNo) {
        return itemRepo.findByCompanyIdAndGroupNoOrderByItemNoAsc(companyId, groupNo)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DownloadResult download(String companyId, Long groupNo, Integer itemNo) {
        FileAttachmentItem item = getItemOwned(companyId, groupNo, itemNo);
        ResponseInputStream<GetObjectResponse> stream =
                s3.getObject(b -> b.bucket(bucket).key(item.getStoragePath()));
        return new DownloadResult(item, stream);
    }

    /** 단건 삭제: 메타 물리 삭제(트랜잭션 내) + 커밋 후 S3 객체 비동기 제거. */
    @Transactional
    public void delete(String companyId, Long groupNo, Integer itemNo) {
        FileAttachmentItem item = getItemOwned(companyId, groupNo, itemNo);
        String key = item.getStoragePath();
        itemRepo.delete(item);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleaner.deleteQuietly(key);
            }
        });
    }

    // ----- helpers -----

    private FileAttachmentItem getItemOwned(String companyId, Long groupNo, Integer itemNo) {
        return itemRepo.findById(new FileAttachmentItemId(companyId, groupNo, itemNo))
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
    }

    private void validate(MultipartFile f) {
        if (f == null || f.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        if (!isMimeAllowed(f.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + f.getContentType());
        }
    }

    private boolean isMimeAllowed(String mime) {
        if (mime == null || allowedMimes.isEmpty()) return false;
        String m = mime.toLowerCase();
        for (String pattern : allowedMimes) {
            String p = pattern.toLowerCase();
            if (p.endsWith("/*")) {
                if (m.startsWith(p.substring(0, p.length() - 1))) return true; // "image/" 접두 일치
            } else if (p.equals(m)) {
                return true;
            }
        }
        return false;
    }

    /** 경로 구분자 제거(traversal 차단) — 원본 파일명 표시용. */
    private String baseName(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String n = name.replace('\\', '/');
        int idx = n.lastIndexOf('/');
        String base = (idx >= 0) ? n.substring(idx + 1) : n;
        base = base.trim();
        if (base.isEmpty() || base.equals(".") || base.equals("..")) return "unnamed";
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        return ext.length() > 10 ? ext.substring(0, 10) : ext;
    }

    private String sanitizeSegment(String seg) {
        if (seg == null || seg.isBlank()) return "common";
        return seg.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("체크섬 계산 실패", e);
        }
    }

    private FileItemResponse toResponse(FileAttachmentItem i) {
        return new FileItemResponse(i.getItemNo(), i.getOriginalFileName(),
                i.getFileExtension(), i.getMimeType(), i.getFileSize());
    }

    /** 다운로드 결과: 메타 + S3 스트림(컨트롤러에서 스트리밍 응답). */
    public record DownloadResult(FileAttachmentItem item, ResponseInputStream<GetObjectResponse> stream) {}
}
