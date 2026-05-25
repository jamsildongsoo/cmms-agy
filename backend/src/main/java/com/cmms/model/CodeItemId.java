package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class CodeItemId implements Serializable {
    private String companyId;
    private String groupId;
    private String id;

    public CodeItemId() {}

    public CodeItemId(String companyId, String groupId, String id) {
        this.companyId = companyId;
        this.groupId = groupId;
        this.id = id;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeItemId that = (CodeItemId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(groupId, that.groupId) &&
               Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, groupId, id);
    }
}
