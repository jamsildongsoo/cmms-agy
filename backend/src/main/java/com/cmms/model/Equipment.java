package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "equipment")
@IdClass(EquipmentId.class)
@Getter
@Setter
public class Equipment extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "plant_id", length = 50)
    private String plantId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "eq_type_code", length = 50)
    private String eqTypeCode;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "work_permit_yn", nullable = false, columnDefinition = "char(1)")
    private String workPermitYn = "N";

    @Column(name = "maker_name", length = 100)
    private String makerName;

    @Column(name = "spec", columnDefinition = "TEXT")
    private String spec;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "file_group_id")
    private Long fileGroupId;
}
