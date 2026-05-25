package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class InventoryMonthlyClosingId implements Serializable {
    private String companyId;
    private String warehouseId;
    private String inventoryId;
    private String closingYm;

    public InventoryMonthlyClosingId() {}

    public InventoryMonthlyClosingId(String companyId, String warehouseId, String inventoryId, String closingYm) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.inventoryId = inventoryId;
        this.closingYm = closingYm;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }
    public String getClosingYm() { return closingYm; }
    public void setClosingYm(String closingYm) { this.closingYm = closingYm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryMonthlyClosingId that = (InventoryMonthlyClosingId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(warehouseId, that.warehouseId) &&
               Objects.equals(inventoryId, that.inventoryId) &&
               Objects.equals(closingYm, that.closingYm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, warehouseId, inventoryId, closingYm);
    }
}
