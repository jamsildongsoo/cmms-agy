package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class InventoryHistoryId implements Serializable {
    private String companyId;
    private String warehouseId;
    private String inventoryId;
    private Long historyNo;

    public InventoryHistoryId() {}

    public InventoryHistoryId(String companyId, String warehouseId, String inventoryId, Long historyNo) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.inventoryId = inventoryId;
        this.historyNo = historyNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }
    public Long getHistoryNo() { return historyNo; }
    public void setHistoryNo(Long historyNo) { this.historyNo = historyNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryHistoryId that = (InventoryHistoryId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(warehouseId, that.warehouseId) &&
               Objects.equals(inventoryId, that.inventoryId) &&
               Objects.equals(historyNo, that.historyNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, warehouseId, inventoryId, historyNo);
    }
}
