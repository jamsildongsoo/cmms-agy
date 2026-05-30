package com.cmms.dto;

import lombok.Getter;
import lombok.Setter;

public class CompanyDto {

    /** 회사 생성 요청 — 회사 정보 + 초기 관리자(ADMIN) 계정을 함께 받는다. */
    @Getter
    @Setter
    public static class CompanyCreateRequest {
        // 회사
        private String id;
        private String name;
        private String businessNumber;
        private String email;
        // 초기 관리자(ADMIN)
        private String adminId;
        private String adminName;
        private String adminPassword;
    }
}
