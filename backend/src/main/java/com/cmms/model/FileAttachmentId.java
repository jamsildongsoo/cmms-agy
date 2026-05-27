package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class FileAttachmentId implements Serializable {
    private String companyId;
    private Long groupNo;

    public FileAttachmentId() {}

    public FileAttachmentId(String companyId, Long groupNo) {
        this.companyId = companyId;
        this.groupNo = groupNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public Long getGroupNo() { return groupNo; }
    public void setGroupNo(Long groupNo) { this.groupNo = groupNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileAttachmentId that = (FileAttachmentId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(groupNo, that.groupNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, groupNo);
    }
}
