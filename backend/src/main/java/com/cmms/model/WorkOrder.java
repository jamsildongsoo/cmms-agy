package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "work_order")
@IdClass(WorkOrderId.class)
@Getter
@Setter
public class WorkOrder extends BaseEntity {

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

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "step_stage", nullable = false, columnDefinition = "char(1)")
    private String stepStage; // P: 계획, R: 실적

    @Column(name = "wo_type_code", nullable = false, length = 50)
    private String woTypeCode;

    @Column(name = "department_id", nullable = false, length = 50)
    private String departmentId;

    @Column(name = "worker_id", length = 50)
    private String workerId;

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "cost", nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "man_hours", nullable = false)
    private BigDecimal manHours = BigDecimal.ZERO;

    @Column(name = "man_hours_unit", nullable = false, length = 10)
    private String manHoursUnit = "H";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "file_group_id")
    private Long fileGroupId;

    @Column(name = "ref_no", length = 50)
    private String refNo;

    @Column(name = "ref_module", length = 50)
    private String refModule;

    @Column(name = "approval_id", length = 50)
    private String approvalId;

    @Column(name = "status", nullable = false, columnDefinition = "char(1)")
    private String status = "T"; // T: 임시저장, P: 결재대기, C: 완결확정(결재완료), S: 직접확정(권한자), R: 반려, X: 취소
}
