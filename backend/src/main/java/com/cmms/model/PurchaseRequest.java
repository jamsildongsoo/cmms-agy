package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "purchase_request")
@IdClass(PurchaseRequestId.class)
@Getter
@Setter
public class PurchaseRequest extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;  // 채번 PUR-{부서}-yyyyMM-####

    @Column(name = "plant_id", nullable = false, length = 50)
    private String plantId;

    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;  // 입고 저장소(헤더 1개, 확정 후 불변)

    @Column(name = "requester_id", nullable = false, length = 50)
    private String requesterId;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "request_type", length = 50)
    private String requestType;  // 공통코드 PR_TYPE 아이템 id

    @Column(name = "vendor_id", length = 50)
    private String vendorId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "eta_date")
    private LocalDate etaDate;

    @Column(name = "ship_start_date")
    private LocalDate shipStartDate;

    // DocStatus: T(저장)/S(직접확정). X/P/C/R 미사용.
    @Column(name = "status", nullable = false, columnDefinition = "char(1)")
    private String status = "T";

    // DocStatus 확장: NULL/O(발주)/D(배송)/I(입고)/E(종료)
    @Column(name = "proc_status", columnDefinition = "char(1)")
    private String procStatus;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
