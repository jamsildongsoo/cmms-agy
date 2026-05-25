package com.cmms.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class LoginHistoryId implements Serializable {
    private String companyId;
    private String userId;
    private LocalDateTime loginAt;

    public LoginHistoryId() {}

    public LoginHistoryId(String companyId, String userId, LocalDateTime loginAt) {
        this.companyId = companyId;
        this.userId = userId;
        this.loginAt = loginAt;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginHistoryId that = (LoginHistoryId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(loginAt, that.loginAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, userId, loginAt);
    }
}
