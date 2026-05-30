package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class PurchaseRequestItemId implements Serializable {
    private String companyId;
    private String requestId;
    private Integer lineNo;

    public PurchaseRequestItemId() {}

    public PurchaseRequestItemId(String companyId, String requestId, Integer lineNo) {
        this.companyId = companyId;
        this.requestId = requestId;
        this.lineNo = lineNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseRequestItemId that = (PurchaseRequestItemId) o;
        return Objects.equals(companyId, that.companyId)
                && Objects.equals(requestId, that.requestId)
                && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, requestId, lineNo);
    }
}
