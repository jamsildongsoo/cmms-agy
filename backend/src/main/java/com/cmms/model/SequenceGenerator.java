package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sequence_generator")
@IdClass(SequenceGeneratorId.class)
@Getter
@Setter
public class SequenceGenerator extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "ref_module", length = 50)
    private String refModule;

    @Id
    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Id
    @Column(name = "year_month", columnDefinition = "char(6)")
    private String yearMonth;

    @Column(name = "last_seq", nullable = false)
    private Integer lastSeq = 0;
}
