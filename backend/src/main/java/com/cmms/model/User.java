package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@IdClass(UserId.class)
@Getter
@Setter
public class User extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "role_id", length = 50)
    private String roleId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "use_yn", nullable = false, columnDefinition = "char(1)")
    private String useYn = "Y";

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_plant_id", length = 50)
    private String lastLoginPlantId;
}
