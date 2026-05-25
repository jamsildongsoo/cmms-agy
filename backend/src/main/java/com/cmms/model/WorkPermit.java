package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_permit")
@IdClass(WorkPermitId.class)
@Getter
@Setter
public class WorkPermit extends BaseEntity {

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

    @Column(name = "work_order_id", length = 50)
    private String workOrderId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "step_stage", nullable = false, columnDefinition = "char(1)")
    private String stepStage; // P: 계획, R: 실적 (허가서발급단계)

    @Column(name = "permit_type_codes", nullable = false, columnDefinition = "TEXT")
    private String permitTypeCodes; // GENERAL,FIRE 등 쉼표 구분

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "department_id", nullable = false, length = 50)
    private String departmentId;

    @Column(name = "supervisor_id", nullable = false, length = 50)
    private String supervisorId;

    @Column(name = "work_summary", columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "safety_measures", columnDefinition = "TEXT")
    private String safety措施; // safety_measures 컬럼

    // JSONB Fields mapped using Hibernate SqlTypes.JSON as String
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_general", columnDefinition = "jsonb")
    private String jsonGeneral;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_fire", columnDefinition = "jsonb")
    private String jsonFire;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_confined", columnDefinition = "jsonb")
    private String jsonConfined;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_electric", columnDefinition = "jsonb")
    private String jsonElectric;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_high_place", columnDefinition = "jsonb")
    private String jsonHighPlace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_excavation", columnDefinition = "jsonb")
    private String jsonExcavation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_heavy_load", columnDefinition = "jsonb")
    private String jsonHeavyLoad;

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
    private String status = "T"; // T: 임시저장, P: 결재대기, C: 완결확정, S: 직접확정, R: 반려, X: 취소

    // getter/setter overrides or translations for safety measures column mapping
    public String getSafetyMeasures() {
        return this.safety措施;
    }

    public void setSafetyMeasures(String safetyMeasures) {
        this.safety措施 = safetyMeasures;
    }
}
