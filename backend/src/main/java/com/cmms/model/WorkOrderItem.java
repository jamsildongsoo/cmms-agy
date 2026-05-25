package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "work_order_item")
@IdClass(WorkOrderItemId.class)
@Getter
@Setter
public class WorkOrderItem {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "plant_id", length = 50)
    private String plantId;

    @Id
    @Column(name = "work_order_id", length = 50)
    private String workOrderId;

    @Id
    @Column(name = "item_no")
    private Integer itemNo;

    @Column(name = "work_name", nullable = false, length = 150)
    private String workName;

    @Column(name = "work_method", length = 250)
    private String workMethod;

    @Column(name = "work_result", columnDefinition = "TEXT")
    private String workResult;
}
