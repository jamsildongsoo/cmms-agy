package com.cmms.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryTxDto {

    @Getter
    @Setter
    public static class TxItem {
        private String warehouseId;
        private String inventoryId;
        private String txTypeCode; // IN: 입고, OUT: 출고, MOVE: 이동, ADJ: 조정
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private String targetWarehouseId; // 이동 시에만 사용
        private LocalDate txDate;
    }

    @Getter
    @Setter
    public static class InventoryTxRequest {
        private List<TxItem> items;
    }
}
