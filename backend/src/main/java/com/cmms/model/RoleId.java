package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class RoleId implements Serializable {
    private String companyId;
    private String id;

    public RoleId() {}

    public RoleId(String companyId, String id) {
        this.companyId = companyId;
        this.id = id;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleId roleId = (RoleId) o;
        return Objects.equals(companyId, roleId.companyId) && Objects.equals(id, roleId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, id);
    }
}
