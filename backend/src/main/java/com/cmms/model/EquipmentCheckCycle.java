package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "equipment_check_cycle")
@IdClass(EquipmentCheckCycleId.class)
@Getter
@Setter
public class EquipmentCheckCycle extends BaseEntity {

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
    @Column(name = "check_type_code", length = 50)
    private String checkTypeCode;

    @Column(name = "cycle_val", nullable = false)
    private Integer cycleVal;

    @Column(name = "cycle_unit", nullable = false, length = 10)
    private String cycleUnit;

    @Column(name = "last_check_date")
    private LocalDate lastCheckDate;

    @Column(name = "next_check_date")
    private LocalDate nextCheckDate;
}
