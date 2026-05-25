package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class InventoryStatusId implements Serializable {
    private String companyId;
    private String warehouseId;
    private String inventoryId;

    public InventoryStatusId() {}

    public InventoryStatusId(String companyId, String warehouseId, String inventoryId) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.inventoryId = inventoryId;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryStatusId that = (InventoryStatusId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(warehouseId, that.warehouseId) &&
               Objects.equals(inventoryId, that.inventoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, warehouseId, inventoryId);
    }
}
