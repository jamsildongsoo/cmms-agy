package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@IdClass(LoginHistoryId.class)
@Getter
@Setter
public class LoginHistory {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Id
    @Column(name = "login_at")
    private LocalDateTime loginAt = LocalDateTime.now();

    @Column(name = "login_ip", length = 50)
    private String loginIp;

    @Column(name = "login_result", nullable = false, length = 20)
    private String loginResult;
}
