package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 파일첨부 그룹. (company_id, group_no) 복합키, group_no는 BIGSERIAL(IDENTITY).
 * 그룹 단위 감사/소프트삭제(BaseEntity). 단건 아이템은 {@link FileAttachmentItem}(물리삭제).
 */
@Entity
@Table(name = "file_attachment")
@IdClass(FileAttachmentId.class)
@Getter
@Setter
public class FileAttachment extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_no")
    private Long groupNo;

    @Column(name = "ref_no", length = 50)
    private String refNo;

    @Column(name = "ref_module", length = 50)
    private String refModule;
}
