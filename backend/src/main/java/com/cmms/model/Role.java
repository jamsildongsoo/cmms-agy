package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role")
@IdClass(RoleId.class)
@Getter
@Setter
public class Role extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "multi_plant", nullable = false, columnDefinition = "char(1)")
    private String multiPlant = "N";
}
