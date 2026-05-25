package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_group")
@IdClass(CodeGroupId.class)
@Getter
@Setter
public class CodeGroup extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "system_use_yn", nullable = false, columnDefinition = "char(1)")
    private String systemUseYn = "N";
}
