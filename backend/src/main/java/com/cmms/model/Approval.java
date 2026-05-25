package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approval")
@IdClass(ApprovalId.class)
@Getter
@Setter
public class Approval extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "drafter_id", nullable = false, length = 50)
    private String drafterId;

    @Column(name = "file_group_id")
    private Long fileGroupId;

    @Column(name = "status", nullable = false, columnDefinition = "char(1)")
    private String status = "T"; // T: 임시저장, P: 진행, C: 완결승인, R: 반려, X: 취소
}
