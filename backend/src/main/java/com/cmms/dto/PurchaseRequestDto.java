package com.cmms.dto;

import com.cmms.model.PurchaseRequest;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseRequestDto {

    @Getter
    @Setter
    public static class ItemLine {
        private Integer lineNo;
        private String inventoryId;
        private BigDecimal qty;
        private String unit;
        private String remarks;
    }

    @Getter
    @Setter
    public static class ReceiveLine {
        private Integer lineNo;
        private BigDecimal qty;
        private BigDecimal unitPrice;  // null 허용 (단가 미입력)
    }

    @Getter
    @Setter
    public static class SaveRequest {
        private PurchaseRequest header;
        private List<ItemLine> items;
        /** true 면 저장 즉시 확정(`status=S`)까지 진행(checkSave A 권한 필요). */
        private boolean confirm;
    }

    @Getter
    @Setter
    public static class OrderRequest {
        private String requestId;
        private String vendorId;
        private LocalDate orderDate;
        private LocalDate etaDate;
    }

    @Getter
    @Setter
    public static class ShipRequest {
        private String requestId;
        private LocalDate shipStartDate;
    }

    @Getter
    @Setter
    public static class ReceiveRequest {
        private String requestId;
        private LocalDate txDate;     // 입고 일자(기본 오늘)
        private boolean close;        // true 면 입고 후 곧바로 proc_status=E
        private List<ReceiveLine> lines;
    }

    @Getter
    @Setter
    public static class RequestDetail {
        private PurchaseRequest header;
        private List<ItemLine> items;
    }
}
