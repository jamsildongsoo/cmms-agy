package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class WorkOrderId implements Serializable {
    private String companyId;
    private String plantId;
    private String id;

    public WorkOrderId() {}

    public WorkOrderId(String companyId, String plantId, String id) {
        this.companyId = companyId;
        this.plantId = plantId;
        this.id = id;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderId that = (WorkOrderId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(plantId, that.plantId) &&
               Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, plantId, id);
    }
}
