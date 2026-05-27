package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 파일첨부 아이템(상세). (company_id, group_no, item_no) 복합키.
 * 감사/소프트삭제 필드 없음 → 단건 삭제는 물리 삭제(스키마상 delete_yn 미보유).
 */
@Entity
@Table(name = "file_attachment_item")
@IdClass(FileAttachmentItemId.class)
@Getter
@Setter
public class FileAttachmentItem {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "group_no")
    private Long groupNo;

    @Id
    @Column(name = "item_no")
    private Integer itemNo;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "checksum_sha256", nullable = false, columnDefinition = "char(64)")
    private String checksumSha256;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
}
