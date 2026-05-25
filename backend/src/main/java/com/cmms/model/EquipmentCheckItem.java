package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "equipment_check_item")
@IdClass(EquipmentCheckItemId.class)
@Getter
@Setter
public class EquipmentCheckItem {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "plant_id", length = 50)
    private String plantId;

    @Id
    @Column(name = "equipment_id", length = 50)
    private String equipmentId;

    @Id
    @Column(name = "item_no")
    private Integer itemNo;

    @Column(name = "check_name", nullable = false, length = 150)
    private String checkName;

    @Column(name = "check_method", length = 250)
    private String checkMethod;

    @Column(name = "min_value", precision = 15, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 15, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "base_value", precision = 15, scale = 4)
    private BigDecimal baseValue;

    @Column(name = "unit", length = 20)
    private String unit;
}
