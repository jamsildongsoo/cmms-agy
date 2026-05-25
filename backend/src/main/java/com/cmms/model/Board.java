package com.cmms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "board")
@IdClass(BoardId.class)
@Getter
@Setter
public class Board extends BaseEntity {

    @Id
    @Column(name = "company_id", length = 50)
    private String companyId;

    @Id
    @Column(name = "id", insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_type_code", nullable = false, length = 50)
    private String boardTypeCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "notice_yn", nullable = false, columnDefinition = "char(1)")
    private String noticeYn = "N"; // Y이면 공지글로 최상단 고정

    @Column(name = "file_group_id")
    private Long fileGroupId;

    @Column(name = "ref_no", length = 50)
    private String refNo;

    @Column(name = "ref_module", length = 50)
    private String refModule;
}
