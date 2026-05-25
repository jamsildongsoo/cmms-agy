package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class PmRecordItemId implements Serializable {
    private String companyId;
    private String plantId;
    private String pmRecordId;
    private Integer itemNo;

    public PmRecordItemId() {}

    public PmRecordItemId(String companyId, String plantId, String pmRecordId, Integer itemNo) {
        this.companyId = companyId;
        this.plantId = plantId;
        this.pmRecordId = pmRecordId;
        this.itemNo = itemNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getPmRecordId() { return pmRecordId; }
    public void setPmRecordId(String pmRecordId) { this.pmRecordId = pmRecordId; }
    public Integer getItemNo() { return itemNo; }
    public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PmRecordItemId that = (PmRecordItemId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(plantId, that.plantId) &&
               Objects.equals(pmRecordId, that.pmRecordId) &&
               Objects.equals(itemNo, that.itemNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, plantId, pmRecordId, itemNo);
    }
}
