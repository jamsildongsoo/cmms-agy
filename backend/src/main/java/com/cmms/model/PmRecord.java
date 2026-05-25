package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "pm_record")
@IdClass(PmRecordId.class)
@Getter
@Setter
public class PmRecord extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "plant_id", length = 50)
    private String plantId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "equipment_id", nullable = false, length = 50)
    private String equipmentId;

    @Column(name = "department_id", nullable = false, length = 50)
    private String departmentId;

    @Column(name = "check_type_code", nullable = false, length = 50)
    private String checkTypeCode;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "worker_id", nullable = false, length = 50)
    private String workerId;

    @Column(name = "judge_code", nullable = false, length = 20)
    private String judgeCode;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "cert_number", length = 100)
    private String certNumber;

    @Column(name = "cert_expire_date")
    private LocalDate certExpireDate;

    @Column(name = "cert_agency", length = 100)
    private String certAgency;

    @Column(name = "approval_id", length = 50)
    private String approvalId;

    @Column(name = "status", nullable = false, columnDefinition = "char(1)")
    private String status = "T"; // T: 임시저장, P: 결재진행, C: 완결확정, S: 직접확정, R: 반려, X: 취소
}
