package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class DepartmentId implements Serializable {
    private String companyId;
    private String id;

    public DepartmentId() {}

    public DepartmentId(String companyId, String id) {
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
        DepartmentId that = (DepartmentId) o;
        return Objects.equals(companyId, that.companyId) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, id);
    }
}
