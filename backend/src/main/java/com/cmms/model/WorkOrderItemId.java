package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class WorkOrderItemId implements Serializable {
    private String companyId;
    private String plantId;
    private String workOrderId;
    private Integer itemNo;

    public WorkOrderItemId() {}

    public WorkOrderItemId(String companyId, String plantId, String workOrderId, Integer itemNo) {
        this.companyId = companyId;
        this.plantId = plantId;
        this.workOrderId = workOrderId;
        this.itemNo = itemNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(String workOrderId) { this.workOrderId = workOrderId; }
    public Integer getItemNo() { return itemNo; }
    public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderItemId that = (WorkOrderItemId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(plantId, that.plantId) &&
               Objects.equals(workOrderId, that.workOrderId) &&
               Objects.equals(itemNo, that.itemNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, plantId, workOrderId, itemNo);
    }
}
