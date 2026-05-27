package com.cmms.dto;

import com.cmms.model.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class SystemDto {

    /** SYSTEM 콘솔 사용자 조회 응답 (passwordHash 미포함). */
    @Getter
    @Setter
    public static class SystemUserResponse {
        private String companyId;
        private String id;
        private String name;
        private String roleId;
        private String departmentId;
        private String position;
        private String title;
        private String email;
        private String phone;
        private String useYn;
        private LocalDateTime lastLoginAt;
        private String lastLoginIp;

        public static SystemUserResponse from(User u) {
            SystemUserResponse r = new SystemUserResponse();
            r.companyId = u.getCompanyId();
            r.id = u.getId();
            r.name = u.getName();
            r.roleId = u.getRoleId();
            r.departmentId = u.getDepartmentId();
            r.position = u.getPosition();
            r.title = u.getTitle();
            r.email = u.getEmail();
            r.phone = u.getPhone();
            r.useYn = u.getUseYn();
            r.lastLoginAt = u.getLastLoginAt();
            r.lastLoginIp = u.getLastLoginIp();
            return r;
        }
    }

    @Getter
    @Setter
    public static class UseYnUpdateRequest {
        private String useYn; // "Y" | "N"
    }
}
