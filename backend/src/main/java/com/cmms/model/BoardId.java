package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class BoardId implements Serializable {
    private String companyId;
    private Long id;

    public BoardId() {}

    public BoardId(String companyId, Long id) {
        this.companyId = companyId;
        this.id = id;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardId boardId = (BoardId) o;
        return Objects.equals(companyId, boardId.companyId) &&
               Objects.equals(id, boardId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, id);
    }
}
