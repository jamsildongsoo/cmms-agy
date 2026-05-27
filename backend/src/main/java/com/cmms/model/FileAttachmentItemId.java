package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class FileAttachmentItemId implements Serializable {
    private String companyId;
    private Long groupNo;
    private Integer itemNo;

    public FileAttachmentItemId() {}

    public FileAttachmentItemId(String companyId, Long groupNo, Integer itemNo) {
        this.companyId = companyId;
        this.groupNo = groupNo;
        this.itemNo = itemNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public Long getGroupNo() { return groupNo; }
    public void setGroupNo(Long groupNo) { this.groupNo = groupNo; }
    public Integer getItemNo() { return itemNo; }
    public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileAttachmentItemId that = (FileAttachmentItemId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(groupNo, that.groupNo) &&
               Objects.equals(itemNo, that.itemNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, groupNo, itemNo);
    }
}
