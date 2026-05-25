package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_item")
@IdClass(CodeItemId.class)
@Getter
@Setter
public class CodeItem {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "group_id", length = 50)
    private String groupId;

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "legal_inspect_yn", nullable = false, columnDefinition = "char(1)")
    private String legalInspectYn = "N";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
