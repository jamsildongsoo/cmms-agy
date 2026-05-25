package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class RoleDetailId implements Serializable {
    private String companyId;
    private String roleId;
    private String moduleDetail;

    public RoleDetailId() {}

    public RoleDetailId(String companyId, String roleId, String moduleDetail) {
        this.companyId = companyId;
        this.roleId = roleId;
        this.moduleDetail = moduleDetail;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getModuleDetail() { return moduleDetail; }
    public void setModuleDetail(String moduleDetail) { this.moduleDetail = moduleDetail; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleDetailId that = (RoleDetailId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(roleId, that.roleId) &&
               Objects.equals(moduleDetail, that.moduleDetail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, roleId, moduleDetail);
    }
}
