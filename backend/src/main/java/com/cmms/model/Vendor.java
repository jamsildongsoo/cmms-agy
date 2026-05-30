package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vendor")
@IdClass(VendorId.class)
@Getter
@Setter
public class Vendor extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "biz_no", length = 50)
    private String bizNo;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "manager", length = 50)
    private String manager;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
