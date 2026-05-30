package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 구매요청 라인(상세). 자식 테이블 — BaseEntity 컬럼 없음, work_order_item 패턴.
 */
@Entity
@Table(name = "purchase_request_item")
@IdClass(PurchaseRequestItemId.class)
@Getter
@Setter
public class PurchaseRequestItem {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "request_id", length = 50)
    private String requestId;

    @Id
    @Column(name = "line_no")
    private Integer lineNo;

    @Column(name = "inventory_id", nullable = false, length = 50)
    private String inventoryId;

    @Column(name = "qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal qty;  // 요청수량(확정 후 불변)

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "received_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal receivedQty = BigDecimal.ZERO;  // 입고 누적(취소 시 차감)

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
