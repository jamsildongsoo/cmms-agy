package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class UserId implements Serializable {
    private String companyId;
    private String id;

    public UserId() {}

    public UserId(String companyId, String id) {
        this.companyId = companyId;
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(companyId, userId.companyId) && Objects.equals(id, userId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, id);
    }
}
