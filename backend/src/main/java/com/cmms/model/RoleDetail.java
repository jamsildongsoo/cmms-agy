package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_detail")
@IdClass(RoleDetailId.class)
@Getter
@Setter
public class RoleDetail {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "role_id", length = 50)
    private String roleId;

    @Id
    @Column(name = "module_detail", length = 100)
    private String moduleDetail;

    @Column(name = "perm_c", nullable = false, columnDefinition = "char(1)")
    private String permC = "N";

    @Column(name = "perm_r", nullable = false, columnDefinition = "char(1)")
    private String permR = "N";

    @Column(name = "perm_u", nullable = false, columnDefinition = "char(1)")
    private String permU = "N";

    @Column(name = "perm_d", nullable = false, columnDefinition = "char(1)")
    private String permD = "N";

    @Column(name = "perm_a", nullable = false, columnDefinition = "char(1)")
    private String permA = "N";
}
