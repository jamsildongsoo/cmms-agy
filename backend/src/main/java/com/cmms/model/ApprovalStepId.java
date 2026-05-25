package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class ApprovalStepId implements Serializable {
    private String companyId;
    private String approvalId;
    private Integer stepNo;

    public ApprovalStepId() {}

    public ApprovalStepId(String companyId, String approvalId, Integer stepNo) {
        this.companyId = companyId;
        this.approvalId = approvalId;
        this.stepNo = stepNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getApprovalId() { return approvalId; }
    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }
    public Integer getStepNo() { return stepNo; }
    public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalStepId that = (ApprovalStepId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(approvalId, that.approvalId) &&
               Objects.equals(stepNo, that.stepNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, approvalId, stepNo);
    }
}
