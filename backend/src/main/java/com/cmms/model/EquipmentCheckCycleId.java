package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class EquipmentCheckCycleId implements Serializable {
    private String companyId;
    private String plantId;
    private String equipmentId;
    private String checkTypeCode;

    public EquipmentCheckCycleId() {}

    public EquipmentCheckCycleId(String companyId, String plantId, String equipmentId, String checkTypeCode) {
        this.companyId = companyId;
        this.plantId = plantId;
        this.equipmentId = equipmentId;
        this.checkTypeCode = checkTypeCode;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getCheckTypeCode() { return checkTypeCode; }
    public void setCheckTypeCode(String checkTypeCode) { this.checkTypeCode = checkTypeCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquipmentCheckCycleId that = (EquipmentCheckCycleId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(plantId, that.plantId) &&
               Objects.equals(equipmentId, that.equipmentId) &&
               Objects.equals(checkTypeCode, that.checkTypeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, plantId, equipmentId, checkTypeCode);
    }
}
