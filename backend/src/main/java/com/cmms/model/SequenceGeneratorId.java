package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class SequenceGeneratorId implements Serializable {
    private String companyId;
    private String refModule;
    private String departmentId;
    private String yearMonth;

    public SequenceGeneratorId() {}

    public SequenceGeneratorId(String companyId, String refModule, String departmentId, String yearMonth) {
        this.companyId = companyId;
        this.refModule = refModule;
        this.departmentId = departmentId;
        this.yearMonth = yearMonth;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getRefModule() { return refModule; }
    public void setRefModule(String refModule) { this.refModule = refModule; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceGeneratorId that = (SequenceGeneratorId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(refModule, that.refModule) &&
               Objects.equals(departmentId, that.departmentId) &&
               Objects.equals(yearMonth, that.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, refModule, departmentId, yearMonth);
    }
}
