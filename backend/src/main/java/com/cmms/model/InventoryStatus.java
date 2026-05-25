package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_status")
@IdClass(InventoryStatusId.class)
@Getter
@Setter
public class InventoryStatus extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;

    @Id
    @Column(name = "inventory_id", length = 50)
    private String inventoryId;

    @Column(name = "qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;
}
