package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_monthly_closing")
@IdClass(InventoryMonthlyClosingId.class)
@Getter
@Setter
public class InventoryMonthlyClosing extends BaseEntity {

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
    @Column(name = "closing_ym", length = 6, columnDefinition = "char(6)")
    private String closingYm;

    @Column(name = "in_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal inQty = BigDecimal.ZERO;

    @Column(name = "in_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal inAmount = BigDecimal.ZERO;

    @Column(name = "out_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal outQty = BigDecimal.ZERO;

    @Column(name = "out_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal outAmount = BigDecimal.ZERO;

    @Column(name = "move_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal moveQty = BigDecimal.ZERO;

    @Column(name = "move_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal moveAmount = BigDecimal.ZERO;

    @Column(name = "adj_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal adjQty = BigDecimal.ZERO;

    @Column(name = "adj_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal adjAmount = BigDecimal.ZERO;

    @Column(name = "closing_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal closingQty = BigDecimal.ZERO;

    @Column(name = "closing_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal closingAmount = BigDecimal.ZERO;
}
