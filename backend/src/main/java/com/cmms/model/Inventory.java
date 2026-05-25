package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory")
@IdClass(InventoryId.class)
@Getter
@Setter
public class Inventory extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "item_type_code", length = 50)
    private String itemTypeCode;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "maker_name", length = 100)
    private String makerName;

    @Column(name = "spec", columnDefinition = "TEXT")
    private String spec;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "safety_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal safetyQty = BigDecimal.ZERO;

    @Column(name = "reorder_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal reorderQty = BigDecimal.ZERO;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays = 0;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "file_group_id")
    private Long fileGroupId;
}
