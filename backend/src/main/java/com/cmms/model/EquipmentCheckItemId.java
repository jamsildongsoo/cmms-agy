package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class EquipmentCheckItemId implements Serializable {
    private String companyId;
    private String plantId;
    private String equipmentId;
    private Integer itemNo;

    public EquipmentCheckItemId() {}

    public EquipmentCheckItemId(String companyId, String plantId, String equipmentId, Integer itemNo) {
        this.companyId = companyId;
        this.plantId = plantId;
        this.equipmentId = equipmentId;
        this.itemNo = itemNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public Integer getItemNo() { return itemNo; }
    public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquipmentCheckItemId that = (EquipmentCheckItemId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(plantId, that.plantId) &&
               Objects.equals(equipmentId, that.equipmentId) &&
               Objects.equals(itemNo, that.itemNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, plantId, equipmentId, itemNo);
    }
}
