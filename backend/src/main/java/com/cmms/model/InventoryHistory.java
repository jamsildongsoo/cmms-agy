package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_history")
@IdClass(InventoryHistoryId.class)
@Getter
@Setter
public class InventoryHistory extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;

    @Id
    @Column(name = "inventory_id", length = 50)
    private String inventoryId;

    @Id
    @Column(name = "history_no", insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyNo;

    @Column(name = "tx_type_code", nullable = false, length = 50)
    private String txTypeCode; // 입고, 출고, 이동, 조정 등

    @Column(name = "qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "ref_no", length = 50)
    private String refNo;

    @Column(name = "ref_module", length = 50)
    private String refModule;

    @Column(name = "doc_no", length = 50)
    private String docNo;  // 전표번호(STK 단일 체계), IN/OUT/MOVE/ADJ 공통
}
