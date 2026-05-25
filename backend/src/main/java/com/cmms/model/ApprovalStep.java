package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_step")
@IdClass(ApprovalStepId.class)
@Getter
@Setter
public class ApprovalStep {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "approval_id", length = 50)
    private String approvalId;

    @Id
    @Column(name = "step_no")
    private Integer stepNo;

    @Column(name = "approver_id", nullable = false, length = 50)
    private String approverId;

    @Column(name = "approval_type", nullable = false, columnDefinition = "char(1)")
    private String approvalType; // D: 기안, A: 결재, G: 합의, R: 참조

    @Column(name = "approval_result", nullable = false, columnDefinition = "char(1)")
    private String approvalResult; // T: 작성/대기, P: 결재대기(진행), A: 승인(완료), R: 반려

    @Column(name = "action_at")
    private LocalDateTime actionAt;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
