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
        private String txTypeCode; // IN/OUT/MOVE/ADJ
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private String targetWarehouseId; // 이동 시에만 사용
        private LocalDate txDate;

        // 전표번호 + 출처 (구매 입고는 docNo=STK, refModule=PUR, refNo=PR번호)
        private String docNo;
        private String refNo;
        private String refModule;
    }

    @Getter
    @Setter
    public static class InventoryTxRequest {
        private List<TxItem> items;
    }
}
